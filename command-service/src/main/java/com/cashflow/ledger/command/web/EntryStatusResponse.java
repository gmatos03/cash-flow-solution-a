package com.cashflow.ledger.command.web;

import java.time.Instant;

public record EntryStatusResponse(
        String entryId,
        String status,
        Instant postedAt
) {
}
