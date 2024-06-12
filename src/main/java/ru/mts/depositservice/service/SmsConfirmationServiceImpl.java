package ru.mts.depositservice.service;

import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import java.util.Random;

@Service
@Scope(value = ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class SmsConfirmationServiceImpl implements ConfirmationService {

    private String openConfirmationCode;                                       // код подтверждения для открытия заявки
    private String closeConfirmationCode;                                      // код подтверждения для закрытия заявки
    private String refillConfirmationCode;                                     // код подтверждения для пополнения заявки

    private final Random random = new Random();

    @Override
    public String generateVerificationCode() {
        int randomNumber = random.nextInt(10_000);
        return String.format("%04d", randomNumber);
    }

    public String getOpenConfirmationCode() {
        return openConfirmationCode;
    }

    public void setOpenConfirmationCode(String openConfirmationCode) {
        this.openConfirmationCode = openConfirmationCode;
    }

    public String getCloseConfirmationCode() {
        return closeConfirmationCode;
    }

    public void setCloseConfirmationCode(String closeConfirmationCode) {
        this.closeConfirmationCode = closeConfirmationCode;
    }

    public String getRefillConfirmationCode() {
        return refillConfirmationCode;
    }

    public void setRefillConfirmationCode(String refillConfirmationCode) {
        this.refillConfirmationCode = refillConfirmationCode;
    }
}
