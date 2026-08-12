package com.cashflow.ledger.reporting.service;

public class AccountNotFoundException extends RuntimeException {
    public AccountNotFoundException(String accountId) {
        super("No account found with accountId=" + accountId);
    }
}
