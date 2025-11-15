package ru.practicum.exception;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.hibernate.exception.ConstraintViolationException;

@Slf4j
@RestControllerAdvice
public class ErrorHandler {

    @ExceptionHandler(NotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleNotFoundException(NotFoundException e) {
        log.error("Not found error: {}", e.getMessage());
        return new ErrorResponse(e.getMessage());
    }

    @ExceptionHandler(ConflictException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse handleConflictException(ConflictException e) {
        log.error("Conflict error: {}", e.getMessage());
        return new ErrorResponse(e.getMessage());
    }

    @ExceptionHandler(ValidationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleValidationException(ValidationException e) {
        log.error("Validation error: {}", e.getMessage());
        return new ErrorResponse(e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleValidationExceptions(MethodArgumentNotValidException ex) {
        String errorMessage = ex.getBindingResult().getFieldError().getDefaultMessage();
        log.error("Validation error: {}", errorMessage);
        return new ErrorResponse(errorMessage);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleIllegalArgumentException(IllegalArgumentException e) {
        log.error("Illegal argument: {}", e.getMessage());
        return new ErrorResponse(e.getMessage());
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleMissingParams(MissingServletRequestParameterException e) {
        log.error("Missing request parameter: {}", e.getMessage());
        return new ErrorResponse("Required request parameter '" + e.getParameterName() + "' is not present");
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse handleDataIntegrityViolation(DataIntegrityViolationException e) {
        log.error("Data integrity violation: {}", e.getMessage());
        if (e.getCause() instanceof ConstraintViolationException) {
            ConstraintViolationException cve = (ConstraintViolationException) e.getCause();
            if (cve.getConstraintName() != null && cve.getConstraintName().contains("email")) {
                return new ErrorResponse("Email already exists");
            }
        }
        return new ErrorResponse("Data integrity violation");
    }

    @ExceptionHandler(jakarta.validation.ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleConstraintViolation(jakarta.validation.ConstraintViolationException e) {
        log.error("Constraint violation: {}", e.getMessage());
        return new ErrorResponse("Constraint violation: " + e.getMessage());
    }

    @ExceptionHandler(org.springframework.transaction.TransactionSystemException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleTransactionException(org.springframework.transaction.TransactionSystemException e) {
        log.error("Transaction error: {}", e.getMessage());
        if (e.getRootCause() instanceof jakarta.validation.ConstraintViolationException) {
            return new ErrorResponse("Validation failed: " + e.getRootCause().getMessage());
        }
        return new ErrorResponse("Transaction error");
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ErrorResponse handleException(Exception e) {
        log.error("Internal server error: {}", e.getMessage(), e);
        return new ErrorResponse("Internal server error: " + e.getMessage());
    }

    @Data
    private static class ErrorResponse {
        private final String error;
        private final String timestamp = java.time.LocalDateTime.now().toString();

        public ErrorResponse(String error) {
            this.error = error;
        }
    }
}