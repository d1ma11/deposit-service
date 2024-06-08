package ru.mts.depositservice.exception;

public class DepositNotFoundException extends CustomException {
    public DepositNotFoundException(String code, String message) {
        super(code, message);
    }
}
