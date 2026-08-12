package com.cashflow.ledger.reporting.domain;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Result of CashFlowAggregationService for a single account/date pair. */
public record CashFlowAggregate(
        String accountId,
        LocalDate reportDate,
        BigDecimal openingBalance,
        BigDecimal totalCredits,
        BigDecimal totalDebits,
        BigDecimal closingBalance
) {
}
