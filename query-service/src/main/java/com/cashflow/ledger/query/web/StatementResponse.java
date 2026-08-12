package com.cashflow.ledger.query.web;

import java.util.List;

public record StatementResponse(
        String accountId,
        List<StatementEntryDto> entries,
        int page,
        int size,
        long totalElements
) {
}
