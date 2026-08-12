package com.cashflow.ledger.command.web;

import com.cashflow.ledger.command.service.CurrencyMismatchException;
import com.cashflow.ledger.command.service.IdempotencyConflictException;
import com.cashflow.ledger.command.service.InvalidAccountException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex,
                                                            HttpServletRequest request) {
        List<ErrorDetail> details = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> new ErrorDetail(fe.getField(), fe.getDefaultMessage()))
                .toList();
        ErrorBody body = new ErrorBody("VALIDATION_ERROR", "One or more fields failed validation", details);
        return ResponseEntity.badRequest().body(ErrorResponse.of(body, request.getRequestURI()));
    }

    @ExceptionHandler(IdempotencyConflictException.class)
    public ResponseEntity<ErrorResponse> handleIdempotencyConflict(IdempotencyConflictException ex,
                                                                     HttpServletRequest request) {
        ErrorBody body = new ErrorBody("IDEMPOTENCY_CONFLICT",
                "This idempotency key was already used; see entryId for the original entry");
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ErrorResponse.withEntryId(body, request.getRequestURI(), ex.getExistingEntryId()));
    }

    @ExceptionHandler(InvalidAccountException.class)
    public ResponseEntity<ErrorResponse> handleInvalidAccount(InvalidAccountException ex,
                                                                HttpServletRequest request) {
        ErrorBody body = new ErrorBody("UNPROCESSABLE_ENTITY", ex.getMessage());
        return ResponseEntity.unprocessableEntity().body(ErrorResponse.of(body, request.getRequestURI()));
    }

    @ExceptionHandler(CurrencyMismatchException.class)
    public ResponseEntity<ErrorResponse> handleCurrencyMismatch(CurrencyMismatchException ex,
                                                                   HttpServletRequest request) {
        ErrorBody body = new ErrorBody("CURRENCY_MISMATCH", ex.getMessage());
        return ResponseEntity.unprocessableEntity().body(ErrorResponse.of(body, request.getRequestURI()));
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentials(BadCredentialsException ex,
                                                                HttpServletRequest request) {
        ErrorBody body = new ErrorBody("UNAUTHORIZED", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ErrorResponse.of(body, request.getRequestURI()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex, HttpServletRequest request) {
        log.error("Unhandled exception on {}: {}", request.getRequestURI(), ex.getMessage(), ex);
        ErrorBody body = new ErrorBody("INTERNAL_ERROR", "An unexpected error occurred");
        return ResponseEntity.internalServerError().body(ErrorResponse.of(body, request.getRequestURI()));
    }
}
