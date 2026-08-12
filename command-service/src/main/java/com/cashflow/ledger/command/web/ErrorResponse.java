package com.cashflow.ledger.command.web;

import java.time.Instant;

/**
 * Standard error envelope shared by every endpoint in Solution A
 * (Appendix F.1). {@code entryId} is only populated for the 409 Conflict
 * response, where the API echoes back the entry that originally used the
 * idempotency key.
 */
public record ErrorResponse(ErrorBody error, Instant timestamp, String path, String entryId) {

    public static ErrorResponse of(ErrorBody error, String path) {
        return new ErrorResponse(error, Instant.now(), path, null);
    }

    public static ErrorResponse withEntryId(ErrorBody error, String path, String entryId) {
        return new ErrorResponse(error, Instant.now(), path, entryId);
    }
}
