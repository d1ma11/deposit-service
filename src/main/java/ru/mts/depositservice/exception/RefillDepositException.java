package ru.mts.depositservice.exception;

public class RefillDepositException extends CustomException {
    public RefillDepositException(String code, String message) {
        super(code, message);
    }
}
