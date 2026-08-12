package com.cashflow.ledger.command.repository;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "accounts")
public class AccountEntity {

    @Id
    @Column(name = "account_id")
    private String accountId;

    @Column(name = "currency", nullable = false)
    private String currency;

    public AccountEntity() {
        // JPA
    }

    public String getAccountId() {
        return accountId;
    }

    public String getCurrency() {
        return currency;
    }
}
