package com.cashflow.ledger.command.web;

import java.time.Instant;
import java.util.UUID;

public record EntryAcceptedResponse(
        String entryId,
        String accountId,
        UUID eventId,
        String status,
        Instant acceptedAt
) {
}
