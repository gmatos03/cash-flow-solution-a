package com.cashflow.ledger.eventhandler.entity;

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

    public LedgerEntryEntity(String entryId, String accountId, String type, BigDecimal amount, String currency,
                              String channel, String description, Instant postedAt, String status) {
        this.entryId = entryId;
        this.accountId = accountId;
        this.type = type;
        this.amount = amount;
        this.currency = currency;
        this.channel = channel;
        this.description = description;
        this.postedAt = postedAt;
        this.status = status;
    }

    public String getEntryId() {
        return entryId;
    }
}
