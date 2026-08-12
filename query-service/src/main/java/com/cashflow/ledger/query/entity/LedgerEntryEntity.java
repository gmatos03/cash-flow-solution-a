package com.cashflow.ledger.query.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "ledger_entries")
public class LedgerEntryEntity {

    @Id
    @Column(name = "entry_id")
    private String entryId;

    @Column(name = "account_id", nullable = false)
    private String accountId;

    @Column(name = "type", nullable = false)
    private String type;

    @Column(name = "amount", nullable = false)
    private BigDecimal amount;

    @Column(name = "currency", nullable = false)
    private String currency;

    @Column(name = "channel", nullable = false)
    private String channel;

    @Column(name = "description")
    private String description;

    @Column(name = "posted_at", nullable = false)
    private Instant postedAt;

    @Column(name = "status", nullable = false)
    private String status;

    public LedgerEntryEntity() {
        // JPA
    }

    public String getEntryId() {
        return entryId;
    }

    public String getAccountId() {
        return accountId;
    }

    public String getType() {
        return type;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getCurrency() {
        return currency;
    }

    public String getChannel() {
        return channel;
    }

    public String getDescription() {
        return description;
    }

    public Instant getPostedAt() {
        return postedAt;
    }

    public String getStatus() {
        return status;
    }
}
