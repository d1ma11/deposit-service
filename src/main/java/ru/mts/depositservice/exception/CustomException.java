package ru.mts.depositservice.exception;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class CustomException extends RuntimeException {
    private String code;
    private String message;

    public CustomException(String code, String message) {
        super(message);
        this.code = code;
        this.message = message;
    }
}
