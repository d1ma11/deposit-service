package ru.mts.depositservice.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.mts.depositservice.client.AccountClient;
import ru.mts.depositservice.entity.Request;
import ru.mts.depositservice.entity.RequestStatus;
import ru.mts.depositservice.enums.RequestStatusEnum;
import ru.mts.depositservice.exception.InvalidConfirmationCodeException;
import ru.mts.depositservice.exception.RequestNotFoundException;
import ru.mts.depositservice.model.ApprovedRequestResponse;
import ru.mts.depositservice.model.OpenDepositRequest;
import ru.mts.depositservice.model.RejectedRequestResponse;
import ru.mts.depositservice.model.RequestResponse;
import ru.mts.depositservice.repository.RequestRepository;
import ru.mts.depositservice.repository.RequestStatusRepository;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class RequestConfirmationService {

    private static final String REJECTION_REASON = "Недостаточно средств для открытия вклада";

    private final AccountClient accountClient;
    private final DepositService depositService;
    private final RequestRepository requestRepository;
    private final RequestStatusService requestStatusService;
    private final RequestStatusRepository requestStatusRepository;

    @Value("${app.sms.confirmation_code}")
    private String confirmationCode;

    @Transactional
    public RequestResponse confirmRequest(OpenDepositRequest openDepositRequest) {
        Optional<Request> optionalRequest = requestRepository.findById(openDepositRequest.getRequestId());
        if (optionalRequest.isEmpty()) {
            throw new RequestNotFoundException(
                    "REQUEST_NOT_FOUND",
                    "Заявка с идентификатором " + openDepositRequest.getRequestId() + " не найдена!"
            );
        }
        Request request = optionalRequest.get();

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
                    .amount(openDepositRequest.getDepositAmount())
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
}
