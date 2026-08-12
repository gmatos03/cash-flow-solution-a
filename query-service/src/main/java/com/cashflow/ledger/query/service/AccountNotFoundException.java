package com.cashflow.ledger.query.service;

public class AccountNotFoundException extends RuntimeException {

    public AccountNotFoundException(String accountId) {
        super("No account found with accountId=" + accountId);
    }
}
