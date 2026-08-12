package com.cashflow.ledger.reporting.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;

@Entity
@Table(name = "accounts")
public class AccountEntity {

    @Id
    @Column(name = "account_id")
    private String accountId;

    @Column(name = "opening_balance", nullable = false)
    private BigDecimal openingBalance;

    @Column(name = "current_balance", nullable = false)
    private BigDecimal currentBalance;

    public AccountEntity() {
        // JPA
    }

    public String getAccountId() {
        return accountId;
    }

    public BigDecimal getOpeningBalance() {
        return openingBalance;
    }

    public BigDecimal getCurrentBalance() {
        return currentBalance;
    }
}
