package ru.mts.depositservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.mts.depositservice.client.AccountClient;
import ru.mts.depositservice.client.CustomerClient;
import ru.mts.depositservice.entity.Customer;
import ru.mts.depositservice.entity.Deposit;
import ru.mts.depositservice.entity.Request;
import ru.mts.depositservice.enums.DepositDurationEnum;
import ru.mts.depositservice.enums.DepositTypeEnum;
import ru.mts.depositservice.enums.PercentPaymentTypeEnum;
import ru.mts.depositservice.exception.DepositNotFoundException;
import ru.mts.depositservice.exception.MinDepositAmountException;
import ru.mts.depositservice.exception.RefillDepositException;
import ru.mts.depositservice.model.OpenDepositRequest;
import ru.mts.depositservice.model.RefillDepositRequest;
import ru.mts.depositservice.property.DepositProperty;
import ru.mts.depositservice.repository.DepositRepository;
import ru.mts.depositservice.repository.DepositTypesRepository;
import ru.mts.depositservice.repository.PercentPaymentTypesRepository;
import ru.mts.depositservice.repository.RequestRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class DepositService {

    private final DepositProperty depositProperty;                                                // базовая процентная ставка
    private final DepositRepository depositRepository;
    private final AccountClient accountClient;
    private final CustomerClient customerClient;
    private final RequestRepository requestRepository;
    private final DepositTypesRepository depositTypesRepository;
    private final SmsConfirmationServiceImpl smsConfirmationService;
    private final PercentPaymentTypesRepository percentPaymentTypesRepository;

    /**
     * Рассчитывает процентную ставку для вклада.
     * <p>
     * Изменяет базовую процентную ставку на основе типа вклада, продолжительности, суммы и капитализации
     *
     * @param request Запрос на открытие вклада с информацией для расчёта ставки
     * @return Расчётная процентная ставка
     */
    public BigDecimal calculateInterestRate(OpenDepositRequest request) {
        BigDecimal rateAdjustment = BigDecimal.valueOf(adjustBaseRate(request));
        return depositProperty.getBaseRate().add(rateAdjustment).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Проверяет минимальную сумму вклада.
     * <p>
     * Если сумма вклада меньше минимально допустимой, генерируется исключение
     *
     * @param depositAmount Сумма вклада для проверки
     */
    public void validateMinimumDepositAmount(BigDecimal depositAmount) {
        if (depositAmount.compareTo(BigDecimal.valueOf(10_000)) < 0) {
            throw new MinDepositAmountException(
                    "MIN_AMOUNT_VIOLATION",
                    "Минимальная сумма для открытия вклада - 10.000"
            );
        }
    }

    /**
     * Открывает новый вклад.
     * <p>
     * Создаёт объект депозита, сохраняет его в репозитории и обновляет связанные запросы
     *
     * @param openDepositRequest Запрос на открытие вклада
     */
    public void openDeposit(OpenDepositRequest openDepositRequest) {
        Deposit deposit = new Deposit();

        // Считываем выбор условий открытия вклада от пользователя
        switch (openDepositRequest.getDepositType()) {
            case DEPOSITS_AND_WITHDRAWALS:
                deposit.setDepositRefill(true);
                deposit.setDepositWithdraw(true);
                break;
            case DEPOSITS_AND_NO_WITHDRAWALS:
                deposit.setDepositRefill(true);
                break;
            case NO_DEPOSITS_AND_WITHDRAWALS:
                break;
        }

        Request request = requestRepository.findById(openDepositRequest.getRequestId()).get();
        Customer customer = customerClient.findCustomer(openDepositRequest.getCustomerId());

        // Рассчитываем по выбранным условиям процентную ставку для вклада
        BigDecimal interestRate = calculateInterestRate(openDepositRequest);
        // Рассчитываем даты начала и окончания договора вклада
        LocalDate startDate = LocalDate.now();
        LocalDate endDate = startDate.plusMonths(getDepositMonthDuration(openDepositRequest.getDuration()));


        // Открываем вклад с выбранными условиями
        deposit.setCapitalization(openDepositRequest.getIsCapitalized());

        deposit.setDepositAmount(request.getAmount());
        accountClient.withdrawMoneyFromAccount(openDepositRequest);                 // списываем деньги с банковского счета

        deposit.setStartDate(startDate);
        deposit.setEndDate(endDate);
        deposit.setDepositRate(interestRate);

        // Если пользователь выбрал капитализацию вклада
        if (!openDepositRequest.getIsCapitalized()) {
            deposit.setPercentPaymentDate(calculatePercentPaymentDate(deposit, openDepositRequest.getPercentPaymentType()));
            deposit.setTypePercentPayment(percentPaymentTypesRepository.findTypesPercentPaymentByTypeName(
                    openDepositRequest.getPercentPaymentType()).get()
            );
            deposit.setPercentPaymentAccount(customer.getBankAccount());
        }

        deposit.setDepositType(depositTypesRepository.findDepositTypesByTypeName(
                openDepositRequest.getDepositType()).get()
        );
        deposit.setBankAccount(customer.getBankAccount());
        deposit.setDepositRefundAccount(customer.getBankAccount());
        deposit.setCustomer(customer);

        depositRepository.save(deposit);

        request.setDeposit(deposit);
        requestRepository.save(request);                                            // связываем заявку с созданным вкладом
    }

    /**
     * Пополняет существующий вклад.
     * <p>
     * Изменяет сумму вклада и процентную ставку, сохраняет изменения в базу данных
     *
     * @param refillRequest Запрос на пополнение вклада
     * @return Обновлённый объект депозита {@link Deposit}
     */
    @Transactional
    public Deposit refillDeposit(RefillDepositRequest refillRequest) {
        Optional<Request> optionalRequest = requestRepository.findById(refillRequest.getRequestId());
        if (optionalRequest.isEmpty()) {
            throw new DepositNotFoundException(
                    "DEPOSIT_NOT_FOUND",
                    "Заявка с идентификатором " + refillRequest.getRequestId() + " не найдена!"
            );
        }
        Deposit deposit = optionalRequest.get().getDeposit();

        // Проверяем условия вклада - можно ли его пополнить?
        if (deposit.isDepositRefill()) {
            BigDecimal currentAmount = deposit.getDepositAmount();                  // текущая сумма денег
            BigDecimal refillAmount = refillRequest.getDepositAmount();             // сумма для снятия

            accountClient.withdrawMoneyFromAccount(refillRequest);                  // списываем деньги с банковского счета
            deposit.setDepositAmount(currentAmount.add(refillAmount));              // кладем деньги на вклад

            BigDecimal currentDepositRate = deposit.getDepositRate();               // текущая процентная ставка
            BigDecimal adjustedDepositRate =                                        // измененная процентная ставка
                    BigDecimal.valueOf(
                            calculateAmountBasedAdjustment(
                                    currentAmount.add(refillAmount))
                    );
            deposit.setDepositRate(currentDepositRate.add(adjustedDepositRate));    // присваиваем новую процентную ставку

            return depositRepository.save(deposit);
        }

        throw new RefillDepositException(
                "REFILL_DEPOSIT_ERROR",
                "Условия вашего вклада не подразумевают его пополнение!"
        );
    }

    /**
     * Найдет все открытые вклады для определённого клиента.
     * <p>
     * Возвращает список всех открытых вкладов для указанного клиента
     *
     * @param customerId Идентификатор клиента
     * @return Список открытых вкладов
     */
    public List<Deposit> findOpenedDeposits(Integer customerId) {
        Customer customer = customerClient.findCustomer(customerId);
        return depositRepository.findDepositsByCustomer(customer);
    }

    private int getDepositMonthDuration(DepositDurationEnum duration) {
        switch (duration) {
            case MONTH_3:
                return DepositDurationEnum.MONTH_3.getDuration();
            case MONTH_6:
                return DepositDurationEnum.MONTH_6.getDuration();
            case YEAR:
                return DepositDurationEnum.YEAR.getDuration();
            default:
                return 0;
        }
    }

    private LocalDate calculatePercentPaymentDate(Deposit deposit, PercentPaymentTypeEnum percentPaymentType) {
        switch (percentPaymentType) {
            case MONTHLY:
                return deposit.getStartDate().plusMonths(1);
            case END_OF_TERM:
                return deposit.getEndDate();
            default:
                throw new IllegalArgumentException("Неподдерживаемый тип выплаты процентов: " + percentPaymentType);
        }
    }

    private double adjustBaseRate(OpenDepositRequest request) {
        double rateAdjustment = 0;
        rateAdjustment += calculateTypeBasedAdjustment(request.getDepositType());
        rateAdjustment += calculateDurationBasedAdjustment(request.getDuration());
        rateAdjustment += calculateAmountBasedAdjustment(request.getDepositAmount());
        rateAdjustment += calculatePercentageTypeBasedAdjustment(request.getIsCapitalized());
        return rateAdjustment;
    }

    private double calculatePercentageTypeBasedAdjustment(boolean isCapitalized) {
        if (isCapitalized) {
            return 0.01;
        }
        return 0;
    }

    private double calculateAmountBasedAdjustment(BigDecimal depositAmount) {
        int count = depositAmount.divide(BigDecimal.valueOf(100_000), RoundingMode.DOWN).intValue();
        if (count == 1) {
            return 0.25;
        } else if (count >= 4) {
            return 0.4;
        }
        return 0;
    }

    private double calculateDurationBasedAdjustment(DepositDurationEnum duration) {
        switch (duration) {
            case MONTH_6:
                return 0.05;
            case YEAR:
                return 0.10;
            default:
                return 0;
        }
    }

    private double calculateTypeBasedAdjustment(DepositTypeEnum depositType) {
        switch (depositType) {
            case DEPOSITS_AND_WITHDRAWALS:
                return 0.05;
            case DEPOSITS_AND_NO_WITHDRAWALS:
                return 0.10;
            case NO_DEPOSITS_AND_WITHDRAWALS:
                return 0.20;
            default:
                return 0;
        }
    }
}
