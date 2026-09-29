package com.ridelink.payment.exception;

import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.Instant;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(PaymentNotFoundException.class)
    ResponseEntity<ApiError> handleNotFound(PaymentNotFoundException exception) {
        return response(HttpStatus.NOT_FOUND, exception.getMessage(), Map.of());
    }

    @ExceptionHandler(InvalidPaymentException.class)
    ResponseEntity<ApiError> handleInvalidPayment(InvalidPaymentException exception) {
        return response(HttpStatus.CONFLICT, exception.getMessage(), Map.of());
    }

    @ExceptionHandler(ResponseStatusException.class)
    ResponseEntity<ApiError> handleResponseStatus(ResponseStatusException exception) {
        HttpStatus status = HttpStatus.valueOf(exception.getStatusCode().value());
        String message = exception.getReason() == null ? status.getReasonPhrase() : exception.getReason();
        return response(status, message, Map.of());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ApiError> handleDataConflict(DataIntegrityViolationException exception) {
        return response(HttpStatus.CONFLICT, "A conflicting payment already exists", Map.of());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException exception) {
        Map<String, String> details = exception.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(error -> error.getField(),
                        error -> error.getDefaultMessage() == null ? "Invalid value" : error.getDefaultMessage(),
                        (first, second) -> first));
        return response(HttpStatus.BAD_REQUEST, "Request validation failed", details);
    }

    @ExceptionHandler({ConstraintViolationException.class, HttpMessageNotReadableException.class})
    ResponseEntity<ApiError> handleBadRequest(Exception exception) {
        return response(HttpStatus.BAD_REQUEST, "Request contains invalid values", Map.of());
    }

    private ResponseEntity<ApiError> response(HttpStatus status, String message, Map<String, String> details) {
        return ResponseEntity.status(status).body(
                new ApiError(Instant.now(), status.value(), message, details));
    }
}
