package com.cashflow.ledger.reporting.web;

import java.time.Instant;

public record ErrorResponse(ErrorBody error, Instant timestamp, String path) {

    public static ErrorResponse of(ErrorBody error, String path) {
        return new ErrorResponse(error, Instant.now(), path);
    }
}
