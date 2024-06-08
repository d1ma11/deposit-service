package ru.mts.depositservice.client;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import ru.mts.depositservice.entity.Customer;

@Service
@RequiredArgsConstructor
public class CustomerClient {

    private static final String CUSTOMER_SERVICE_NAME = "customer-service";
    private static final String FIND_CUSTOMER = "/customer/{customerId}";

    private final DiscoveryClient discoveryClient;
    private final RestTemplate restTemplate;

    private ServiceInstance serviceInstance;

    @PostConstruct
    public void init() {
        serviceInstance = discoveryClient.getInstances(CUSTOMER_SERVICE_NAME)
                .stream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(CUSTOMER_SERVICE_NAME + " сервис недоступен"));
    }

    public Customer findCustomer(Integer customerId) {
        UriComponentsBuilder uriComponentsBuilder = UriComponentsBuilder
                .fromHttpUrl(serviceInstance.getUri().toString() + FIND_CUSTOMER);

        ResponseEntity<Customer> response = restTemplate.exchange(
                uriComponentsBuilder.buildAndExpand(customerId).toUri(),
                HttpMethod.GET,
                null,
                Customer.class
        );

        return response.getBody();
    }
}
