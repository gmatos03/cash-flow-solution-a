package com.cashflow.ledger.query.web;

import java.math.BigDecimal;
import java.time.Instant;

public record StatementEntryDto(
        String entryId,
        String type,
        BigDecimal amount,
        String currency,
        Instant postedAt,
        String description
) {
}
