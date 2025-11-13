package ru.practicum.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import jakarta.validation.ConstraintViolationException;
import java.time.LocalDateTime;

@Slf4j
@RestControllerAdvice
public class ErrorHandler {

    @ExceptionHandler(NotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiError handleNotFound(NotFoundException e) {
        log.warn("Not found: {}", e.getMessage());
        return new ApiError("NOT_FOUND", "The required object was not found.", e.getMessage(), LocalDateTime.now());
    }

    @ExceptionHandler(ConflictException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiError handleConflict(ConflictException e) {
        log.warn("Conflict: {}", e.getMessage());
        return new ApiError("CONFLICT", "Integrity constraint has been violated.", e.getMessage(), LocalDateTime.now());
    }

    @ExceptionHandler(ValidationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError handleValidation(ValidationException e) {
        log.warn("Validation error: {}", e.getMessage());
        return new ApiError("BAD_REQUEST", "Incorrectly made request.", e.getMessage(), LocalDateTime.now());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError handleValidationError(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> String.format("Field: %s. Error: %s. Value: %s",
                        error.getField(),
                        error.getDefaultMessage(),
                        error.getRejectedValue()))
                .findFirst()
                .orElse("Validation error");

        log.warn("Method argument validation error: {}", message);
        return new ApiError("BAD_REQUEST", "Incorrectly made request.", message, LocalDateTime.now());
    }

    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError handleConstraintViolation(ConstraintViolationException ex) {
        String message = ex.getConstraintViolations().stream()
                .map(violation -> String.format("Field: %s. Error: %s. Value: %s",
                        violation.getPropertyPath(),
                        violation.getMessage(),
                        violation.getInvalidValue()))
                .findFirst()
                .orElse("Constraint violation error");

        log.warn("Constraint violation error: {}", message);
        return new ApiError("BAD_REQUEST", "Incorrectly made request.", message, LocalDateTime.now());
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError handleMissingParams(MissingServletRequestParameterException ex) {
        String message = String.format("Parameter '%s' is missing", ex.getParameterName());
        log.warn("Missing parameter: {}", message);
        return new ApiError("BAD_REQUEST", "Incorrectly made request.", message, LocalDateTime.now());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        String message = String.format("Parameter '%s' has invalid value: '%s'",
                ex.getName(), ex.getValue());
        log.warn("Type mismatch: {}", message);
        return new ApiError("BAD_REQUEST", "Incorrectly made request.", message, LocalDateTime.now());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError handleIllegalArgument(IllegalArgumentException ex) {
        log.warn("Illegal argument: {}", ex.getMessage());
        return new ApiError("BAD_REQUEST", "Incorrectly made request.", ex.getMessage(), LocalDateTime.now());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiError handleDataIntegrityViolation(DataIntegrityViolationException ex) {
        log.warn("Data integrity violation: {}", ex.getMessage());
        return new ApiError("CONFLICT", "Integrity constraint has been violated.",
                "Database constraint violation", LocalDateTime.now());
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiError handleError(Exception e) {
        log.error("Internal server error: ", e);
        return new ApiError("INTERNAL_SERVER_ERROR", "Internal server error.",
                "Something went wrong", LocalDateTime.now());
    }
}
