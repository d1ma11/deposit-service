package ru.mts.depositservice.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import ru.mts.depositservice.client.AccountClient;
import ru.mts.depositservice.client.CustomerClient;
import ru.mts.depositservice.entity.*;
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
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DepositServiceTest {

    @Mock
    DepositRepository depositRepository;
    @Mock
    AccountClient accountClient;
    @Mock
    CustomerClient customerClient;
    @Mock
    DepositProperty depositProperty;
    @Mock
    RequestRepository requestRepository;
    @Mock
    DepositTypesRepository depositTypesRepository;
    @Mock
    PercentPaymentTypesRepository percentPaymentTypesRepository;

    @InjectMocks
    DepositService depositService;

    private Deposit deposit;
    private Request request;
    private RefillDepositRequest refillRequest;

    @BeforeEach
    void setUp() {
        doReturn(new BigDecimal("5")).when(depositProperty).getBaseRate();

        refillRequest = new RefillDepositRequest();
        refillRequest.setRequestId(1);
        refillRequest.setDepositAmount(BigDecimal.valueOf(5000));

        deposit = new Deposit();
        deposit.setDepositAmount(BigDecimal.valueOf(10_000));
        deposit.setDepositRefill(true);
        deposit.setDepositRate(BigDecimal.valueOf(5.11));

        request = new Request();
        request.setDeposit(deposit);
    }

    @Test
    void calculateInterestRate() {
        OpenDepositRequest request = new OpenDepositRequest();
        request.setDepositAmount(BigDecimal.valueOf(15000));
        request.setDuration(DepositDurationEnum.MONTH_6);
        request.setDepositType(DepositTypeEnum.DEPOSITS_AND_WITHDRAWALS);
        request.setIsCapitalized(true);

        when(depositTypesRepository.findDepositTypesByTypeName(any())).thenReturn(Optional.of(new DepositTypes()));
        when(percentPaymentTypesRepository.findTypesPercentPaymentByTypeName(any())).thenReturn(Optional.of(new TypesPercentPayment()));

        BigDecimal result = depositService.calculateInterestRate(request);

        assertNotNull(result);

        BigDecimal expectedRate = BigDecimal.valueOf(5.11).setScale(2, RoundingMode.HALF_UP);
        assertEquals(expectedRate, result);
    }

    @Test
    void validateMinimumDepositAmount_validAmount() {
        BigDecimal depositAmount = BigDecimal.valueOf(20000);

        assertDoesNotThrow(() -> depositService.validateMinimumDepositAmount(depositAmount));
    }

    @Test
    void validateMinimumDepositAmount_invalidAmount() {
        BigDecimal depositAmount = BigDecimal.valueOf(5000);

        assertThrows(MinDepositAmountException.class, () -> depositService.validateMinimumDepositAmount(depositAmount));
    }

    @Test
    void openDeposit() {
        OpenDepositRequest openDepositRequest = new OpenDepositRequest();
        openDepositRequest.setRequestId(1);
        openDepositRequest.setCustomerId(1);
        openDepositRequest.setDepositType(DepositTypeEnum.DEPOSITS_AND_WITHDRAWALS);
        openDepositRequest.setDuration(DepositDurationEnum.YEAR);
        openDepositRequest.setDepositAmount(BigDecimal.valueOf(100_000));
        openDepositRequest.setIsCapitalized(true);
        openDepositRequest.setPercentPaymentType(PercentPaymentTypeEnum.MONTHLY);

        request = new Request();
        request.setAmount(BigDecimal.valueOf(100_000));

        Customer customer = new Customer();
        customer.setId(1);
        customer.setBankAccount(new BankAccount());

        when(requestRepository.findById(anyInt())).thenReturn(Optional.of(request));
        when(customerClient.findCustomer(anyInt())).thenReturn(customer);
        when(depositTypesRepository.findDepositTypesByTypeName(any())).thenReturn(Optional.of(new DepositTypes(1, DepositTypeEnum.DEPOSITS_AND_WITHDRAWALS)));
        when(percentPaymentTypesRepository.findTypesPercentPaymentByTypeName(any())).thenReturn(Optional.of(new TypesPercentPayment(1, PercentPaymentTypeEnum.MONTHLY)));
        when(depositRepository.save(any(Deposit.class))).thenAnswer(invocation -> invocation.getArgument(0));

        depositService.openDeposit(openDepositRequest);

        verify(accountClient).withdrawMoneyFromAccount(openDepositRequest);
        verify(depositRepository).save(any(Deposit.class));
        verify(requestRepository).save(any(Request.class));
    }

    @Test
    void refillDeposit() {
        when(requestRepository.findById(anyInt())).thenReturn(Optional.of(request));
        when(depositRepository.findById(anyInt())).thenReturn(Optional.of(deposit));
        when(depositRepository.save(any(Deposit.class))).thenReturn(deposit);

        Deposit result = depositService.refillDeposit(refillRequest);

        assertNotNull(result);
        assertEquals(result.getDepositAmount(), BigDecimal.valueOf(15_000));
    }

    @Test
    public void testRefillDepositNotFound() {
        when(requestRepository.findById(any())).thenReturn(Optional.empty());

        Exception exception = assertThrows(DepositNotFoundException.class, () -> depositService.refillDeposit(refillRequest));

        String expectedMessage = "Заявка с идентификатором 1 не найдена!";
        String actualMessage = exception.getMessage();

        assertTrue(actualMessage.contains(expectedMessage));
    }

    @Test
    public void testRefillDepositNotAllowed() {
        deposit.setDepositRefill(false);

        when(requestRepository.findById(any())).thenReturn(Optional.of(request));

        Exception exception = assertThrows(RefillDepositException.class, () -> depositService.refillDeposit(refillRequest));

        String expectedMessage = "Условия вашего вклада не подразумевают его пополнение!";
        String actualMessage = exception.getMessage();

        assertTrue(actualMessage.contains(expectedMessage));
    }

    @Test
    void findOpenedDeposits() {
        Integer customerId = 1;
        when(customerClient.findCustomer(customerId)).thenReturn(new Customer());
        when(depositRepository.findDepositsByCustomer(any(Customer.class))).thenReturn(List.of(new Deposit()));

        List<Deposit> deposits = depositService.findOpenedDeposits(customerId);

        assertNotNull(deposits);
        assertFalse(deposits.isEmpty());
    }
}