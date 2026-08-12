package com.cashflow.ledger.reporting.web;

import java.time.Instant;
import java.time.LocalDate;

public record ReportRunResponse(
        LocalDate reportDate,
        int accountsProcessed,
        Instant completedAt
) {
}
