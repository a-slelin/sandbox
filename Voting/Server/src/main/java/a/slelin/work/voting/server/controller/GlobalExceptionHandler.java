package a.slelin.work.voting.server.controller;

import a.slelin.work.voting.common.dto.ErrorResponse;
import a.slelin.work.voting.server.exception.VotingException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Превращает любые ошибки в единый JSON-формат {@link ErrorResponse}.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(VotingException.class)
    public ResponseEntity<ErrorResponse> handleVoting(VotingException e, HttpServletRequest request) {
        return build(e.getStatus(), e.getMessage(), request, null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException e,
                                                          HttpServletRequest request) {
        Map<String, String> details = new LinkedHashMap<>();
        e.getBindingResult().getFieldErrors()
                .forEach(error -> details.putIfAbsent(error.getField(), error.getDefaultMessage()));
        return build(HttpStatus.BAD_REQUEST, "Некорректные данные запроса", request, details);
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<ErrorResponse> handleBadRequest(Exception e, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, "Некорректный формат запроса", request, null);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleOther(Exception e, HttpServletRequest request) {
        // Стандартные исключения Spring MVC (404 на неизвестный путь, 405 и т.д.) сами знают свой статус.
        if (e instanceof org.springframework.web.ErrorResponse springError) {
            HttpStatusCode status = springError.getStatusCode();
            return build(HttpStatus.valueOf(status.value()), e.getMessage(), request, null);
        }

        log.error("Unexpected error on {} {}", request.getMethod(), request.getRequestURI(), e);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Внутренняя ошибка сервера", request, null);
    }

    private ResponseEntity<ErrorResponse> build(HttpStatus status, String message,
                                                HttpServletRequest request, Map<String, String> details) {
        return ResponseEntity.status(status).body(ErrorResponse.builder()
                .status(status.value())
                .error(status.getReasonPhrase())
                .message(message)
                .path(request.getRequestURI())
                .details(details)
                .timestamp(Instant.now())
                .build());
    }
}
