package ru.mts.depositservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@EnableDiscoveryClient
@SpringBootApplication
public class DepositServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(DepositServiceApplication.class, args);
    }

}
