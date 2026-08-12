package com.cashflow.ledger.command.repository;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "event_store")
public class EventStoreEntity {

    @Id
    @Column(name = "event_id")
    private UUID eventId;

    @Column(name = "entry_id", nullable = false)
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

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(name = "schema_version", nullable = false)
    private Integer schemaVersion;

    protected EventStoreEntity() {
        // JPA
    }

    public EventStoreEntity(UUID eventId, String entryId, String accountId, String type, BigDecimal amount,
                             String currency, String channel, String description, Instant occurredAt,
                             Integer schemaVersion) {
        this.eventId = eventId;
        this.entryId = entryId;
        this.accountId = accountId;
        this.type = type;
        this.amount = amount;
        this.currency = currency;
        this.channel = channel;
        this.description = description;
        this.occurredAt = occurredAt;
        this.schemaVersion = schemaVersion;
    }

    public UUID getEventId() {
        return eventId;
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

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public Integer getSchemaVersion() {
        return schemaVersion;
    }
}
