package ru.mts.depositservice.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import ru.mts.depositservice.client.AccountClient;
import ru.mts.depositservice.entity.Deposit;
import ru.mts.depositservice.entity.DepositTypes;
import ru.mts.depositservice.entity.Request;
import ru.mts.depositservice.entity.RequestStatus;
import ru.mts.depositservice.enums.DepositTypeEnum;
import ru.mts.depositservice.exception.DepositNotFoundException;
import ru.mts.depositservice.exception.InvalidConfirmationCodeException;
import ru.mts.depositservice.exception.RequestNotFoundException;
import ru.mts.depositservice.model.*;
import ru.mts.depositservice.repository.DepositRepository;
import ru.mts.depositservice.repository.RequestRepository;
import ru.mts.depositservice.repository.RequestStatusRepository;

import java.math.BigDecimal;
import java.util.Date;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RequestConfirmationServiceTest {

    @Mock
    AccountClient accountClient;
    @Mock
    DepositService depositService;
    @Mock
    DepositRepository depositRepository;
    @Mock
    RequestRepository requestRepository;
    @Mock
    RequestStatusService requestStatusService;
    @Mock
    RequestStatusRepository requestStatusRepository;
    @Mock
    SmsConfirmationServiceImpl smsConfirmationService;

    @InjectMocks
    private RequestConfirmationService requestConfirmationService;

    @Test
    void confirmOpenRequest_success() {
        OpenDepositRequest openRequest = new OpenDepositRequest();
        openRequest.setRequestId(1);
        openRequest.setConfirmationCode("1337");
        openRequest.setDepositAmount(BigDecimal.valueOf(10000));
        openRequest.setCustomerId(1);

        Request request = new Request();
        request.setId(1);
        request.setAmount(BigDecimal.valueOf(10000));

        given(requestRepository.findById(any())).willReturn(Optional.of(request));
        given(requestStatusRepository.findRequestStatusByStatusName(any())).willReturn(new RequestStatus());
        given(smsConfirmationService.getOpenConfirmationCode()).willReturn("1337");
        given(accountClient.checkEnoughMoney(any())).willReturn(true);

        RequestResponse result = requestConfirmationService.confirmOpenRequest(openRequest);

        verify(requestStatusService, times(2)).changeCurrentRequestStatus(any(), any());
        verify(depositService).openDeposit(any());
        assertThat(result).isNotNull();
    }

    @Test
    void confirmOpenRequest_requestNotFound() {
        OpenDepositRequest openRequest = new OpenDepositRequest();
        openRequest.setRequestId(1);
        openRequest.setConfirmationCode("1337");
        openRequest.setDepositAmount(BigDecimal.valueOf(10000));
        openRequest.setCustomerId(1);

        Request request = new Request();
        request.setId(1);
        request.setAmount(BigDecimal.valueOf(10000));

        given(requestRepository.findById(any())).willReturn(Optional.empty());
        given(requestStatusRepository.findRequestStatusByStatusName(any())).willReturn(new RequestStatus());
        given(smsConfirmationService.getOpenConfirmationCode()).willReturn("1337");
        given(accountClient.checkEnoughMoney(any())).willReturn(true);

        assertThrows(RequestNotFoundException.class, () -> requestConfirmationService.confirmOpenRequest(openRequest));
    }

    @Test
    void confirmRefillDeposit_success() {
        RefillDepositRequest refillRequest = new RefillDepositRequest();
        refillRequest.setConfirmationCode("1337");
        refillRequest.setDepositAmount(BigDecimal.valueOf(10_000));

        Deposit deposit = new Deposit();
        deposit.setDepositAmount(BigDecimal.valueOf(10_000));
        deposit.setDepositType(new DepositTypes(1, DepositTypeEnum.DEPOSITS_AND_NO_WITHDRAWALS));

        given(smsConfirmationService.getRefillConfirmationCode()).willReturn("1337");
        given(accountClient.checkEnoughMoney(refillRequest)).willReturn(true);
        given(depositService.refillDeposit(refillRequest)).willReturn(deposit);
        given(requestRepository.findById(anyInt())).willReturn(Optional.of(new Request()));

        RequestResponse result = requestConfirmationService.confirmRefillDeposit(refillRequest);

        verify(depositService).refillDeposit(refillRequest);
        assertThat(result).isNotNull();
        assertThat(result.getRequestDate()).isNotNull();
        assertThat(result.getAmount()).isEqualTo(refillRequest.getDepositAmount());
    }

    @Test
    void confirmRefillDeposit_insufficientFunds() {
        RefillDepositRequest refillRequest = new RefillDepositRequest();
        refillRequest.setConfirmationCode("1337");
        refillRequest.setDepositAmount(BigDecimal.valueOf(5000));

        given(smsConfirmationService.getRefillConfirmationCode()).willReturn("1337");
        given(accountClient.checkEnoughMoney(refillRequest)).willReturn(false);

        RejectedRefillResponse result = (RejectedRefillResponse) requestConfirmationService.confirmRefillDeposit(refillRequest);

        verify(depositService, never()).refillDeposit(refillRequest);
        assertThat(result).isNotNull();
        assertThat(result.getRequestDate()).isNotNull();
        assertThat(result.getRejectionReason()).isEqualTo("Недостаточно средств для открытия вклада");
    }


    @Test
    void confirmCloseDeposit_success() {
        CloseDepositRequest closeRequest = new CloseDepositRequest();
        closeRequest.setConfirmationCode("1337");
        closeRequest.setRequestId(1);

        given(smsConfirmationService.getCloseConfirmationCode()).willReturn("1337");
        given(requestRepository.findById(closeRequest.getRequestId())).willReturn(Optional.of(new Request()));
        given(requestRepository.findById(anyInt())).willReturn(Optional.of(new Request(
                1,
                new Date(),
                BigDecimal.valueOf(10_000),
                null,
                new Deposit()
        )));

        requestConfirmationService.confirmCloseDeposit(closeRequest);

        verify(accountClient).refillAccount(any());
        verify(depositRepository).delete(any(Deposit.class));
    }

    @Test
    void confirmCloseDeposit_invalidConfirmationCode() {
        CloseDepositRequest closeRequest = new CloseDepositRequest();
        closeRequest.setConfirmationCode("wrongCode");
        closeRequest.setRequestId(1);

        given(smsConfirmationService.getCloseConfirmationCode()).willReturn("1337");

        assertThrows(InvalidConfirmationCodeException.class, () -> requestConfirmationService.confirmCloseDeposit(closeRequest));
    }

    @Test
    void confirmCloseDeposit_depositNotFound() {
        CloseDepositRequest closeRequest = new CloseDepositRequest();
        closeRequest.setConfirmationCode("1337");
        closeRequest.setRequestId(1);

        given(smsConfirmationService.getCloseConfirmationCode()).willReturn("1337");
        given(requestRepository.findById(closeRequest.getRequestId())).willReturn(Optional.empty());

        assertThrows(DepositNotFoundException.class, () -> requestConfirmationService.confirmCloseDeposit(closeRequest));
    }

}