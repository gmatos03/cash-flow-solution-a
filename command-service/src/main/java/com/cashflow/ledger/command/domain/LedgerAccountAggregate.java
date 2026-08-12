package com.cashflow.ledger.command.domain;

import java.time.Instant;
import java.util.UUID;

/**
 * Minimal domain model for the write side. In the full CQRS design the
 * authoritative balance lives on the read side (projected by the Event
 * Handler Service); this aggregate is only responsible for turning a valid
 * command into a domain event, which keeps the write path fast and free of
 * cross-service reads.
 */
public final class LedgerAccountAggregate {

    private LedgerAccountAggregate() {
    }

    public static CreditDebitEvent apply(PostEntryCommand cmd) {
        return new CreditDebitEvent(
                UUID.randomUUID(),
                cmd.entryId(),
                cmd.accountId(),
                cmd.type(),
                cmd.amount(),
                cmd.currency(),
                cmd.channel(),
                cmd.description(),
                Instant.now(),
                1
        );
    }
}
