package com.cashflow.ledger.command.web;

import java.util.List;

public record ErrorBody(String code, String message, List<ErrorDetail> details) {

    public ErrorBody(String code, String message) {
        this(code, message, List.of());
    }
}
