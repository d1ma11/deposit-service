package ru.mts.depositservice.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.mts.depositservice.client.CustomerClient;
import ru.mts.depositservice.entity.Customer;
import ru.mts.depositservice.entity.Request;
import ru.mts.depositservice.entity.RequestStatus;
import ru.mts.depositservice.enums.RequestStatusEnum;
import ru.mts.depositservice.model.OpenDepositRequest;
import ru.mts.depositservice.repository.RequestRepository;
import ru.mts.depositservice.repository.RequestStatusRepository;

import java.util.Date;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RequestService {

    private final DepositService depositService;
    private final CustomerClient customerClient;
    private final RequestRepository requestRepository;
    private final RequestStatusRepository requestStatusRepository;

    private final RequestStatusService requestStatusService;

    @Transactional
    public void makeRequest(OpenDepositRequest openDepositRequest) {
        depositService.validateMinimumDepositAmount(openDepositRequest.getDepositAmount());

        RequestStatus confirmingStatus = requestStatusRepository.findRequestStatusByStatusName(RequestStatusEnum.CONFIRMING);
        Customer customer = customerClient.findCustomer(openDepositRequest.getCustomerId());

        Request request = new Request();
        request.setCustomerId(customer);
        request.setRequestDate(new Date());

        request = requestRepository.save(request);

        requestStatusService.changeCurrentRequestStatus(request, confirmingStatus);
    }

    public List<Request> findRejectedRequests(Integer customerId) {
        return requestRepository.findRequestsByCustomerId(customerId);
    }
}
