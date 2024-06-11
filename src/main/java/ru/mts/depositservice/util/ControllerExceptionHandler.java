package ru.mts.depositservice.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import ru.mts.depositservice.exception.CustomException;
import ru.mts.depositservice.model.ExceptionData;
import ru.mts.depositservice.model.ExceptionResponse;

@Slf4j
@ControllerAdvice
public class ControllerExceptionHandler extends ResponseEntityExceptionHandler {

    /**
     * Обрабатывает исключения типа {@link CustomException}.
     * <p>
     * Возвращает объект {@link ExceptionResponse<ExceptionData>} с информацией об ошибке и статусом HTTP 400 (BAD REQUEST)
     *
     * @param e Исключение, которое нужно обработать
     * @return Ответ с информацией об ошибке и статусом HTTP
     */
    @ExceptionHandler(CustomException.class)
    public ResponseEntity<ExceptionResponse<ExceptionData>> handleException(CustomException e) {
        ExceptionResponse<ExceptionData> exceptionResponse =
                new ExceptionResponse<>(new ExceptionData(e.getCode(), e.getMessage()));

        log.error("Произошла ошибка: {}", e.getMessage());

        return new ResponseEntity<>(exceptionResponse, HttpStatus.BAD_REQUEST);
    }
}
