package com.cashflow.ledger.query.service;

import java.time.LocalDate;

public class ReportNotFoundException extends RuntimeException {

    public ReportNotFoundException(String accountId, LocalDate reportDate) {
        super("No daily cash-flow log found for accountId=" + accountId + " on " + reportDate);
    }
}
