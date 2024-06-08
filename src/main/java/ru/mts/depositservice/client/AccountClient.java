package ru.mts.depositservice.client;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import ru.mts.depositservice.entity.BankAccount;
import ru.mts.depositservice.entity.Customer;
import ru.mts.depositservice.entity.Request;
import ru.mts.depositservice.model.DepositRequest;
import ru.mts.depositservice.model.InfoResponse;
import ru.mts.depositservice.model.UserRequest;
import ru.mts.depositservice.repository.RequestRepository;

import java.math.BigDecimal;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class AccountClient {

    private static final String ACCOUNT_SERVICE_NAME = "account-service";
    private static final String CHECK_ENOUGH_MONEY = "/account/check";
    private static final String GET_ACCOUNT_MONEY = "/account/{accountId}";
    private static final String WITHDRAW_MONEY = "/account/withdraw";
    private static final String REFILL_MONEY = "/account/refill";

    private final RestTemplate restTemplate;
    private final CustomerClient customerClient;
    private final DiscoveryClient discoveryClient;
    private final RequestRepository requestRepository;

    private ServiceInstance serviceInstance;

    @PostConstruct
    public void init() {
        serviceInstance = discoveryClient.getInstances(ACCOUNT_SERVICE_NAME)
                .stream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(ACCOUNT_SERVICE_NAME + " сервис недоступен"));
    }

    public boolean checkEnoughMoney(DepositRequest depositRequest) {
        Request request = requestRepository.findById(depositRequest.getRequestId()).get();
        Customer customer = customerClient.findCustomer(request.getCustomer().getId());

        UserRequest userRequest = createUserRequest(customer.getBankAccount().getId(), request.getAmount());

        ResponseEntity<Boolean> response = restTemplate.postForEntity(
                buildUri(CHECK_ENOUGH_MONEY),
                userRequest,
                Boolean.class
        );

        return Boolean.TRUE.equals(response.getBody());
    }

    public BigDecimal getAccountMoney(Integer customerId) {
        ResponseEntity<InfoResponse> response = restTemplate.exchange(
                buildUri(GET_ACCOUNT_MONEY, customerId),
                HttpMethod.GET,
                null,
                InfoResponse.class
        );

        return Objects.requireNonNull(response.getBody()).getAmount();
    }

    public void withdrawMoneyFromAccount(DepositRequest depositRequest) {
        executePatchRequest(WITHDRAW_MONEY, depositRequest);
    }

    public void refillAccount(DepositRequest depositRequest) {
        executePatchRequest(REFILL_MONEY, depositRequest);
    }

    private void executePatchRequest(String endpointPath, DepositRequest depositRequest) {
        Request request = requestRepository.findById(depositRequest.getRequestId()).get();
        Customer customer = customerClient.findCustomer(request.getCustomer().getId());
        BankAccount bankAccount = customerClient.findCustomer(customer.getId()).getBankAccount();
        UserRequest userRequest = createUserRequest(bankAccount.getId(), depositRequest.getDepositAmount());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<UserRequest> requestEntity = new HttpEntity<>(userRequest, headers);

        restTemplate.exchange(
                buildUri(endpointPath),
                HttpMethod.PATCH,
                requestEntity,
                InfoResponse.class
        );
    }

    private String buildUri(String endpointPath, Object... uriVariables) {
        return UriComponentsBuilder.fromHttpUrl(serviceInstance.getUri().toString() + endpointPath)
                .buildAndExpand(uriVariables)
                .toUriString();
    }

    private UserRequest createUserRequest(Integer accountId, BigDecimal money) {
        UserRequest userRequest = new UserRequest();
        userRequest.setAccountId(accountId);
        userRequest.setMoney(money);
        return userRequest;
    }
}
