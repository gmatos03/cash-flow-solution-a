package com.cashflow.ledger.eventhandler.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Mirrors com.cashflow.ledger.command.domain.CreditDebitEvent field-for-field
 * so Spring Kafka's JsonDeserializer can map the Command Service's JSON
 * payload onto this service's own copy of the type (Appendix F.5 schema).
 */
public record CreditDebitEvent(
        UUID eventId,
        String entryId,
        String accountId,
        EntryType type,
        BigDecimal amount,
        String currency,
        String channel,
        String description,
        Instant occurredAt,
        int schemaVersion
) {
}
