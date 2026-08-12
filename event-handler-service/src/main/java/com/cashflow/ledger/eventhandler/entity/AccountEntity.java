package com.cashflow.ledger.eventhandler.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "accounts")
public class AccountEntity {

    @Id
    @Column(name = "account_id")
    private String accountId;

    @Column(name = "account_name", nullable = false)
    private String accountName;

    @Column(name = "currency", nullable = false)
    private String currency;

    @Column(name = "opening_balance", nullable = false)
    private BigDecimal openingBalance;

    @Column(name = "current_balance", nullable = false)
    private BigDecimal currentBalance;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public AccountEntity() {
        // JPA
    }

    public String getAccountId() {
        return accountId;
    }

    public BigDecimal getCurrentBalance() {
        return currentBalance;
    }
}
