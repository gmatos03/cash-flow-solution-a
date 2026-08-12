package com.cashflow.ledger.query.web;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DailyCashFlowLogResponse(
        String accountId,
        LocalDate reportDate,
        BigDecimal openingBalance,
        BigDecimal totalCredits,
        BigDecimal totalDebits,
        BigDecimal closingBalance,
        String reconciliationStatus
) {
}
