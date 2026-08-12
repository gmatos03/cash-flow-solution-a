package com.cashflow.ledger.reporting.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;

/** Minimal read-only mapping - only the columns CashFlowAggregationService's queries need. */
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

    @Column(name = "posted_at", nullable = false)
    private Instant postedAt;

    public LedgerEntryEntity() {
        // JPA
    }
}
