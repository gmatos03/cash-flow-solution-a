package com.cashflow.ledger.command.domain;

import java.math.BigDecimal;

/**
 * Internal command object built by the controller from the incoming
 * {@code EntryRequest} and handed to {@link com.cashflow.ledger.command.service.PostEntryCommandHandler}.
 * Mirrors the PostEntryCommand DTO shown in Appendix B's C4 Level 4 diagram.
 */
public record PostEntryCommand(
        String entryId,
        String accountId,
        BigDecimal amount,
        String currency,
        EntryType type,
        String channel,
        String description,
        String idempotencyKey
) {
}
