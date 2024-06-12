package ru.mts.depositservice.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.mts.depositservice.client.AccountClient;
import ru.mts.depositservice.entity.Deposit;
import ru.mts.depositservice.entity.Request;
import ru.mts.depositservice.entity.RequestStatus;
import ru.mts.depositservice.enums.RequestStatusEnum;
import ru.mts.depositservice.exception.DepositNotFoundException;
import ru.mts.depositservice.exception.InvalidConfirmationCodeException;
import ru.mts.depositservice.exception.RequestNotFoundException;
import ru.mts.depositservice.model.*;
import ru.mts.depositservice.repository.DepositRepository;
import ru.mts.depositservice.repository.RequestRepository;
import ru.mts.depositservice.repository.RequestStatusRepository;

import java.util.Date;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class RequestConfirmationService {

    private static final String REJECTION_REASON = "Недостаточно средств для открытия вклада";

    private final AccountClient accountClient;
    private final RequestService requestService;
    private final DepositService depositService;
    private final DepositRepository depositRepository;
    private final RequestRepository requestRepository;
    private final RequestStatusService requestStatusService;
    private final RequestStatusRepository requestStatusRepository;
    private final SmsConfirmationServiceImpl smsConfirmationService;

    /**
     * Подтверждает заявку на открытие депозита.
     * <p>
     * Проверяет наличие заявки, корректность кода подтверждения, достаточность средств на счете клиента и изменяет статус заявки соответственно
     *
     * @param openRequest Объект запроса на открытие депозита с подтверждающим кодом
     * @return Ответ на процесс подтверждения заявки {@link RequestResponse}
     */
    @Transactional
    public RequestResponse confirmOpenRequest(OpenDepositRequest openRequest) {
        // Находим заявку в базе данных
        Optional<Request> optionalConfirmingRequest = requestRepository.findById(openRequest.getRequestId());
        if (optionalConfirmingRequest.isEmpty()) {
            throw new RequestNotFoundException(
                    "REQUEST_NOT_FOUND",
                    "Заявка с идентификатором " + openRequest.getRequestId() + " не найдена!"
            );
        }
        Request confirmingRequest = optionalConfirmingRequest.get();

        // Устанавливаем сумму в DTO заявке ту, которую пользователь указывал при создании заявки на открытие вклада
        openRequest.setDepositAmount(confirmingRequest.getAmount());

        // Проверяем код подтверждения на правильность
        if (openRequest.getConfirmationCode().equals(smsConfirmationService.getOpenConfirmationCode())) {
            RequestStatus confirmedStatus = requestStatusRepository.findRequestStatusByStatusName(RequestStatusEnum.CONFIRMED);
            requestStatusService.changeCurrentRequestStatus(confirmingRequest, confirmedStatus);    // меняем статус заявки на "ПОДТВЕРЖДЕНО"
        } else {
            throw new InvalidConfirmationCodeException(
                    "INVALID_SMS_CODE",
                    "Неправильный код подтверждения!"
            );
        }

        // Проверяем лежит ли необходимая сумма на банковском счете клиента
        if (accountClient.checkEnoughMoney(openRequest)) {
            RequestStatus approvedStatus = requestStatusRepository
                    .findRequestStatusByStatusName(RequestStatusEnum.APPROVED);
            requestStatusService.changeCurrentRequestStatus(confirmingRequest, approvedStatus);     // меняем статус заявки на "ОДОБРЕНО"

            depositService.openDeposit(openRequest);                                                // открываем вклад

            return ApprovedRequestResponse.builder()
                    .requestId(openRequest.getRequestId())
                    .depositTypeEnum(openRequest.getDepositType())
                    .amount(confirmingRequest.getAmount())
                    .requestDate(confirmingRequest.getRequestDate())
                    .percentageRate(depositService.calculateInterestRate(openRequest))
                    .build();
        }

        // При отсутствии необходимой суммы на банковском счете клиента
        RequestStatus rejectedStatus = requestStatusRepository.findRequestStatusByStatusName(RequestStatusEnum.REJECTED);
        requestStatusService.changeCurrentRequestStatus(confirmingRequest, rejectedStatus);         // меняем статус заявки на "ОТКЛОНЕНО"

        return RejectedRequestResponse.builder()
                .amount(accountClient.getAccountMoney(openRequest.getCustomerId()))
                .requestDate(confirmingRequest.getRequestDate())
                .rejectionReason(REJECTION_REASON)
                .build();
    }

    /**
     * Подтверждает заявку на пополнение депозита.
     * <p>
     * Проверяет корректность кода подтверждения и достаточность средств на счете клиента, затем выполняет пополнение депозита
     *
     * @param refillRequest Объект запроса на пополнение депозита с подтверждающим кодом
     * @return Ответ на процесс подтверждения заявки {@link RequestResponse}
     */
    @Transactional
    public RequestResponse confirmRefillDeposit(RefillDepositRequest refillRequest) {
        // Проверяем код подтверждения на правильность
        if (!refillRequest.getConfirmationCode().equals(smsConfirmationService.getRefillConfirmationCode())) {
            throw new InvalidConfirmationCodeException(
                    "INVALID_SMS_CODE",
                    "Неправильный код подтверждения!"
            );
        }

        // Проверяем есть ли необходимая сумма денег на счету
        if (accountClient.checkEnoughMoney(refillRequest)) {
            Deposit deposit = depositService.refillDeposit(refillRequest);                          // пополнение вклада

            return RequestResponse.builder()
                    .requestDate(new Date())
                    .amount(deposit.getDepositAmount())
                    .build();
        }

        return RejectedRefillResponse.builder()
                .requestDate(new Date())
                .amount(refillRequest.getDepositAmount())
                .rejectionReason(REJECTION_REASON)
                .build();
    }

    /**
     * Подтверждает заявку на закрытие депозита
     * <p>
     * Проверяет корректность кода подтверждения и находит соответствующую заявку в базе данных,
     * после чего закрывает депозит
     */
    @Transactional
    public void confirmCloseDeposit(CloseDepositRequest closeRequest) {
        // Проверяем код подтверждения на правильность
        if (!closeRequest.getConfirmationCode().equals(smsConfirmationService.getCloseConfirmationCode())) {
            throw new InvalidConfirmationCodeException(
                    "INVALID_SMS_CODE",
                    "Неправильный код подтверждения!"
            );
        }

        // Находим заявку в базе данных
        Optional<Request> optionalRequest = requestRepository.findById(closeRequest.getRequestId());

        if (optionalRequest.isEmpty()) {
            throw new DepositNotFoundException(
                    "DEPOSIT_NOT_FOUND",
                    "Заявка с идентификатором " + closeRequest.getRequestId() + " не найдена!"
            );
        }
        Request request = optionalRequest.get();

        // Закрываем вклад
        Deposit deposit = request.getDeposit();                                                     // находим счет вклада
        closeRequest.setDepositAmount(deposit.getDepositAmount());                                  // в запросе пишем, что снимаем все деньги
        accountClient.refillAccount(closeRequest);                                                  // выполняется запрос на снятие денег
        depositRepository.delete(deposit);                                                          // закрывается счет вклада
    }
}
