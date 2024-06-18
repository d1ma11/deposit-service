package ru.mts.depositservice.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
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

        log.error("Произошла ошибка: {}, Код ошибки: {}, Сообщение ошибки: {}",
                e.getClass().getSimpleName(),
                e.getCode(),
                e.getMessage()
        );

        return new ResponseEntity<>(exceptionResponse, HttpStatus.BAD_REQUEST);
    }

    /**
     * Обрабатывает исключения типа ConstraintViolationException путем преобразования исключения
     * в соответствующий ответ с HTTP статусом BAD_REQUEST
     *
     * @param e исключение, которое необходимо обработать
     * @return ResponseEntity с информацией об ошибке и статусом BAD_REQUEST
     */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException e,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {

        String errorMessage = "Validation failed";

        for (ObjectError error : e.getBindingResult().getAllErrors()) {
            if (error instanceof FieldError fieldError) {
                if ("depositAmount".equals(fieldError.getField())) {
                    errorMessage = fieldError.getDefaultMessage();
                }
            }
        }

        ExceptionResponse<ExceptionData> exceptionResponse =
                new ExceptionResponse<>(new ExceptionData("VALIDATION_ERROR", errorMessage));

        log.error("Произошла ошибка: {}, Код ошибки: {}, Сообщение ошибки: {}", e.getClass().getSimpleName(), "VALIDATION_ERROR", errorMessage);

        return new ResponseEntity<>(exceptionResponse, HttpStatus.BAD_REQUEST);
    }
}
