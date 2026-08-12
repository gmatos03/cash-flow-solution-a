package com.cashflow.ledger.command.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * The domain event appended to the event store and published to Kafka.
 * Field layout matches the JSON schema documented in Appendix F.5.
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
