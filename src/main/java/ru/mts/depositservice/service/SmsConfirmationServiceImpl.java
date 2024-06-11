package ru.mts.depositservice.service;

import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import java.util.Random;

@Service
@Scope(value = ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class SmsConfirmationServiceImpl implements ConfirmationService {

    private final Random random = new Random();

    @Override
    public String generateVerificationCode() {
        int randomNumber = random.nextInt(10_000);
        return String.format("%04d", randomNumber);
    }
}
