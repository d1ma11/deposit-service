package ru.mts.depositservice.exception;

public class MinDepositAmountException extends CustomException {
    public MinDepositAmountException(String code, String message) {
        super(code, message);
    }
}
