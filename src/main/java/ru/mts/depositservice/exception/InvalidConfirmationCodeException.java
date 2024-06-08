package ru.mts.depositservice.exception;

public class InvalidConfirmationCodeException extends CustomException {
    public InvalidConfirmationCodeException(String code, String message) {
        super(code, message);
    }
}
