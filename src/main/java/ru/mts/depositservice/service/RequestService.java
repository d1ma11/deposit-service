package ru.mts.depositservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.mts.depositservice.client.CustomerClient;
import ru.mts.depositservice.entity.Customer;
import ru.mts.depositservice.entity.Deposit;
import ru.mts.depositservice.entity.Request;
import ru.mts.depositservice.entity.RequestStatus;
import ru.mts.depositservice.enums.RequestStatusEnum;
import ru.mts.depositservice.exception.DepositNotFoundException;
import ru.mts.depositservice.exception.InvalidConfirmationCodeException;
import ru.mts.depositservice.model.CloseDepositRequest;
import ru.mts.depositservice.model.OpenDepositRequest;
import ru.mts.depositservice.repository.RequestRepository;
import ru.mts.depositservice.repository.RequestStatusRepository;

import java.util.Date;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class RequestService {

    private static String OPEN_CONFIRMATION_CODE;
    private static String CLOSE_CONFIRMATION_CODE;
    private static String REFILL_CONFIRMATION_CODE;

    private final DepositService depositService;

    private final CustomerClient customerClient;
    private final RequestRepository requestRepository;
    private final RequestStatusRepository requestStatusRepository;
    private final SmsConfirmationServiceImpl smsConfirmationService;
    private final RequestStatusService requestStatusService;

    @Transactional
    public void openRequest(OpenDepositRequest openDepositRequest) {
        depositService.validateMinimumDepositAmount(openDepositRequest.getDepositAmount());

        OPEN_CONFIRMATION_CODE = smsConfirmationService.generateVerificationCode();
        log.info("Код {} для операции открытия вклада", OPEN_CONFIRMATION_CODE);

        RequestStatus confirmingStatus = requestStatusRepository.findRequestStatusByStatusName(RequestStatusEnum.CONFIRMING);
        Customer customer = customerClient.findCustomer(openDepositRequest.getCustomerId());

        Request request = new Request();
        request.setCustomer(customer);
        request.setAmount(openDepositRequest.getDepositAmount());
        request.setRequestDate(new Date());

        request = requestRepository.save(request);

        requestStatusService.changeCurrentRequestStatus(request, confirmingStatus);
    }

    public void refillRequest() {
        REFILL_CONFIRMATION_CODE = smsConfirmationService.generateVerificationCode();
        log.info("Код {} для операции пополнения вклада", REFILL_CONFIRMATION_CODE);
    }

    public void closeRequest() {
        CLOSE_CONFIRMATION_CODE = smsConfirmationService.generateVerificationCode();
        log.info("Код {} для операции закрытия вклада", CLOSE_CONFIRMATION_CODE);
    }

    public List<Request> findRejectedRequests(Integer customerId) {
        return requestRepository.findRequestsByCustomerId(customerId);
    }

    public String getCloseConfirmationCode() {
        return CLOSE_CONFIRMATION_CODE;
    }

    public String getOpenConfirmationCode() {
        return OPEN_CONFIRMATION_CODE;
    }

    public String getRefillConfirmationCode() {
        return REFILL_CONFIRMATION_CODE;
    }
}
