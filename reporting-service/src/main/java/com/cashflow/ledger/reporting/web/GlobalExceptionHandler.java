package com.cashflow.ledger.reporting.web;

import com.cashflow.ledger.reporting.service.AccountNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(AccountNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleAccountNotFound(AccountNotFoundException ex, HttpServletRequest request) {
        ErrorBody body = new ErrorBody("NOT_FOUND", ex.getMessage());
        return ResponseEntity.status(404).body(ErrorResponse.of(body, request.getRequestURI()));
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentials(BadCredentialsException ex, HttpServletRequest request) {
        ErrorBody body = new ErrorBody("UNAUTHORIZED", ex.getMessage());
        return ResponseEntity.status(401).body(ErrorResponse.of(body, request.getRequestURI()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex, HttpServletRequest request) {
        log.error("Unhandled exception on {}: {}", request.getRequestURI(), ex.getMessage(), ex);
        ErrorBody body = new ErrorBody("INTERNAL_ERROR", "An unexpected error occurred");
        return ResponseEntity.internalServerError().body(ErrorResponse.of(body, request.getRequestURI()));
    }
}
