package com.cashflow.ledger.command.service;

public class IdempotencyConflictException extends RuntimeException {

    private final String existingEntryId;

    public IdempotencyConflictException(String existingEntryId) {
        super("Idempotency key was already used for entryId=" + existingEntryId);
        this.existingEntryId = existingEntryId;
    }

    public String getExistingEntryId() {
        return existingEntryId;
    }
}
