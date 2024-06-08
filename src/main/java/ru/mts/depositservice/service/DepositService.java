package ru.mts.depositservice.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import ru.mts.depositservice.client.AccountClient;
import ru.mts.depositservice.client.CustomerClient;
import ru.mts.depositservice.entity.Customer;
import ru.mts.depositservice.entity.Deposit;
import ru.mts.depositservice.entity.Request;
import ru.mts.depositservice.enums.DepositDurationEnum;
import ru.mts.depositservice.enums.PercentPaymentTypeEnum;
import ru.mts.depositservice.exception.DepositNotFoundException;
import ru.mts.depositservice.exception.MinDepositAmountException;
import ru.mts.depositservice.model.OpenDepositRequest;
import ru.mts.depositservice.model.RefillDepositRequest;
import ru.mts.depositservice.repository.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class DepositService {

    @Value("${app.deposit.base_rate}")
    private BigDecimal baseRate; // базовая процентная ставка
    private final DepositRepository depositRepository;
    private final AccountClient accountClient;
    private final CustomerClient customerClient;
    private final RequestRepository requestRepository;
    private final DepositTypesRepository depositTypesRepository;
    private final PercentPaymentTypesRepository percentPaymentTypesRepository;

    public BigDecimal calculateInterestRate(OpenDepositRequest request) {
        BigDecimal rateAdjustment = BigDecimal.valueOf(adjustBaseRate(request));
        return baseRate.add(rateAdjustment).setScale(2, RoundingMode.HALF_UP);
    }

    public void validateMinimumDepositAmount(BigDecimal depositAmount) throws MinDepositAmountException {
        if (depositAmount.compareTo(BigDecimal.valueOf(10_000)) < 0) {
            throw new MinDepositAmountException(
                    "MIN_AMOUNT_VIOLATION",
                    "Минимальная сумма для открытия вклада - 10.000"
            );
        }
    }

    public void openDeposit(OpenDepositRequest openDepositRequest) {
        Deposit deposit = new Deposit();

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

        deposit.setCapitalization(openDepositRequest.isCapitalized());
        deposit.setDepositAmount(openDepositRequest.getDepositAmount());
        deposit.setStartDate(LocalDate.now());
        deposit.setEndDate(deposit.getStartDate().plusMonths(getDepositMonthDuration(openDepositRequest.getDuration())));
        deposit.setDepositRate(calculateInterestRate(openDepositRequest));
        if (!openDepositRequest.isCapitalized()) {
            deposit.setPercentPaymentDate(calculatePercentPaymentDate(deposit, openDepositRequest.getPercentPaymentType()));
        }
        deposit.setDepositType(depositTypesRepository.findDepositTypesByTypeName(
                openDepositRequest.getDepositType()).get()
        );
        deposit.setTypePercentPayment(percentPaymentTypesRepository.findTypesPercentPaymentByTypeName(
                openDepositRequest.getPercentPaymentType()).get()
        );
        Customer customer = customerClient.findCustomer(openDepositRequest.getCustomerId());
        deposit.setBankAccount(customer.getBankAccount());
        deposit.setPercentPaymentAccount(customer.getBankAccount());
        deposit.setDepositRefundAccount(customer.getBankAccount());
        deposit.setCustomer(customer);

        depositRepository.save(deposit);

        accountClient.withdrawMoneyFromAccount(openDepositRequest);

        Request request = requestRepository.findById(openDepositRequest.getRequestId()).get();
        request.setDepositId(deposit);
        requestRepository.save(request);
    }

    public void refillAccount(RefillDepositRequest request) {
        Optional<Deposit> optionalDeposit = depositRepository.findById(request.getRequestId());

        if (optionalDeposit.isPresent()) {
            Deposit deposit = optionalDeposit.get();

            if (accountClient.checkEnoughMoney(request)) {

            }
        } else {
            throw new DepositNotFoundException(
                    "DEPOSIT_NOT_FOUND",
                    "Вклад с идентификатором " + request.getRequestId() + " не найдена!"
            );
        }
    }

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
        rateAdjustment += calculateTypeBasedAdjustment(request);
        rateAdjustment += calculateDurationBasedAdjustment(request);
        rateAdjustment += calculateAmountBasedAdjustment(request);
        rateAdjustment += calculatePercentageTypeBasedAdjustment(request);
        return rateAdjustment;
    }

    private double calculatePercentageTypeBasedAdjustment(OpenDepositRequest request) {
        if (request.isCapitalized()) {
            return 0.01;
        }
        return 0;
    }

    private double calculateAmountBasedAdjustment(OpenDepositRequest request) {
        int a = request.getDepositAmount().divide(BigDecimal.valueOf(100_000), RoundingMode.DOWN).intValue();
        if (a == 1) {
            return 0.25;
        } else if (a <= 4) {
            return 0.4;
        }
        return 0;
    }

    private double calculateDurationBasedAdjustment(OpenDepositRequest request) {
        switch (request.getDuration()) {
            case MONTH_6:
                return 0.05;
            case YEAR:
                return 0.10;
            default:
                return 0;
        }
    }

    private double calculateTypeBasedAdjustment(OpenDepositRequest request) {
        switch (request.getDepositType()) {
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
