package ru.mts.depositservice.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.mts.depositservice.client.CustomerClient;
import ru.mts.depositservice.entity.Customer;
import ru.mts.depositservice.entity.Request;
import ru.mts.depositservice.entity.RequestStatus;
import ru.mts.depositservice.enums.RequestStatusEnum;
import ru.mts.depositservice.model.OpenDepositRequest;
import ru.mts.depositservice.repository.RequestRepository;
import ru.mts.depositservice.repository.RequestStatusRepository;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RequestServiceTest {

    @Mock
    DepositService depositService;
    @Mock
    CustomerClient customerClient;
    @Mock
    RequestRepository requestRepository;
    @Mock
    RequestStatusRepository requestStatusRepository;
    @Mock
    SmsConfirmationServiceImpl smsConfirmationService;
    @Mock
    RequestStatusService requestStatusService;

    @InjectMocks
    RequestService requestService;

    @Test
    void openRequest() {
        OpenDepositRequest request = new OpenDepositRequest();
        request.setCustomerId(1);
        request.setDepositAmount(BigDecimal.valueOf(10_000));

        Customer customer = new Customer();
        customer.setId(1);
        when(customerClient.findCustomer(any())).thenReturn(customer);

        RequestStatus confirmingStatus = new RequestStatus();
        confirmingStatus.setStatusName(RequestStatusEnum.CONFIRMING);
        when(requestStatusRepository.findRequestStatusByStatusName(any())).thenReturn(confirmingStatus);

        doNothing().when(depositService).validateMinimumDepositAmount(any());

        requestService.openRequest(request);

        verify(depositService, times(1)).validateMinimumDepositAmount(any());
        verify(smsConfirmationService, times(1)).generateVerificationCode();
        verify(requestRepository, times(1)).save(any());
        verify(requestStatusService, times(1)).changeCurrentRequestStatus(any(), any());
    }

    @Test
    void refillRequest() {
        requestService.refillRequest();

        verify(smsConfirmationService, times(1)).generateVerificationCode();
    }

    @Test
    void closeRequest() {
        requestService.closeRequest();

        verify(smsConfirmationService, times(1)).generateVerificationCode();
    }

    @Test
    void findRejectedRequests() {
        Integer customerId = 1;

        Customer customer = new Customer(1, "phoneNumber", null);

        List<Request> rejectedRequests = Arrays.asList(
                new Request(1, new Date(), BigDecimal.valueOf(10000), customer, null),
                new Request(2, new Date(), BigDecimal.valueOf(10000), customer, null)
        );

        when(requestRepository.findRequestsByCustomerId(eq(customerId))).thenReturn(rejectedRequests);

        List<Request> result = requestService.findRejectedRequests(customerId);

        assertEquals(rejectedRequests, result);

        verify(requestRepository).findRequestsByCustomerId(eq(customerId));
    }
}