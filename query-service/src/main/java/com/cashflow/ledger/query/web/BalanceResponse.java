package com.cashflow.ledger.query.web;

import java.math.BigDecimal;
import java.time.Instant;

public record BalanceResponse(
        String accountId,
        BigDecimal currentBalance,
        String currency,
        Instant asOf,
        String source
) {
    public BalanceResponse withSource(String newSource) {
        return new BalanceResponse(accountId, currentBalance, currency, asOf, newSource);
    }
}
