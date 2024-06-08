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

    @Transactional
    public RequestResponse confirmOpenRequest(OpenDepositRequest openDepositRequest) {
        String confirmationCode = requestService.getOpenConfirmationCode();

        Optional<Request> optionalRequest = requestRepository.findById(openDepositRequest.getRequestId());
        if (optionalRequest.isEmpty()) {
            throw new RequestNotFoundException(
                    "REQUEST_NOT_FOUND",
                    "Заявка с идентификатором " + openDepositRequest.getRequestId() + " не найдена!"
            );
        }

        Request request = optionalRequest.get();
        openDepositRequest.setDepositAmount(request.getAmount());

        if (openDepositRequest.getConfirmationCode().equals(confirmationCode)) {
            RequestStatus confirmedStatus = requestStatusRepository.findRequestStatusByStatusName(RequestStatusEnum.CONFIRMED);

            requestStatusService.changeCurrentRequestStatus(request, confirmedStatus);
        } else {
            throw new InvalidConfirmationCodeException(
                    "INVALID_SMS_CODE",
                    "Неправильный код подтверждения!"
            );
        }

        if (accountClient.checkEnoughMoney(openDepositRequest)) {
            RequestStatus approvedStatus = requestStatusRepository.findRequestStatusByStatusName(RequestStatusEnum.APPROVED);
            requestStatusService.changeCurrentRequestStatus(request, approvedStatus);

            depositService.openDeposit(openDepositRequest);

            return ApprovedRequestResponse.builder()
                    .requestId(openDepositRequest.getRequestId())
                    .depositTypeEnum(openDepositRequest.getDepositType())
                    .amount(request.getAmount())
                    .requestDate(request.getRequestDate())
                    .percentageRate(depositService.calculateInterestRate(openDepositRequest))
                    .build();
        }
        RequestStatus rejectedStatus = requestStatusRepository.findRequestStatusByStatusName(RequestStatusEnum.REJECTED);
        requestStatusService.changeCurrentRequestStatus(request, rejectedStatus);

        return RejectedRequestResponse.builder()
                .amount(accountClient.getAccountMoney(openDepositRequest.getCustomerId()))
                .requestDate(request.getRequestDate())
                .rejectionReason(REJECTION_REASON)
                .build();
    }

    @Transactional
    public RequestResponse confirmRefillDeposit(RefillDepositRequest refillRequest) {
        if (!refillRequest.getConfirmationCode().equals(requestService.getRefillConfirmationCode())) {
            throw new InvalidConfirmationCodeException(
                    "INVALID_SMS_CODE",
                    "Неправильный код подтверждения!"
            );
        }

        // Проверяем есть ли необходимая сумма денег на счету
        if (accountClient.checkEnoughMoney(refillRequest)) {
            Deposit deposit = depositService.refillDeposit(refillRequest);

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

    @Transactional
    public void confirmCloseDeposit(CloseDepositRequest closeRequest) {
        if (!closeRequest.getConfirmationCode().equals(requestService.getCloseConfirmationCode())) {
            throw new InvalidConfirmationCodeException(
                    "INVALID_SMS_CODE",
                    "Неправильный код подтверждения!"
            );
        }

        Optional<Request> optionalRequest = requestRepository.findById(closeRequest.getRequestId());

        if (optionalRequest.isEmpty()) {
            throw new DepositNotFoundException(
                    "DEPOSIT_NOT_FOUND",
                    "Заявка с идентификатором " + closeRequest.getRequestId() + " не найдена!"
            );
        }
        Request request = optionalRequest.get();

        Deposit deposit = request.getDeposit();                                         // находим счет вклада
        closeRequest.setDepositAmount(deposit.getDepositAmount());                      // в запросе пишем, что снимаем все деньги
        accountClient.refillAccount(closeRequest);                                      // выполняется запрос на снятие денег
        depositRepository.delete(deposit);                                              // закрывается счет вклада

        RequestStatus confirmedStatus = requestStatusRepository.findRequestStatusByStatusName(RequestStatusEnum.CONFIRMED);
        requestStatusService.changeCurrentRequestStatus(request, confirmedStatus);
    }
}
