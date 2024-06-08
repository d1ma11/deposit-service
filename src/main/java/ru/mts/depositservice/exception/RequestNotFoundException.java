package ru.mts.depositservice.exception;

public class RequestNotFoundException extends CustomException {
    public RequestNotFoundException(String code, String message) {
        super(code, message);
    }
}
