package com.cashflow.ledger.query.entity;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Objects;

public class DailyCashFlowLogId implements Serializable {

    private String accountId;
    private LocalDate reportDate;

    public DailyCashFlowLogId() {
    }

    public DailyCashFlowLogId(String accountId, LocalDate reportDate) {
        this.accountId = accountId;
        this.reportDate = reportDate;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DailyCashFlowLogId that)) return false;
        return Objects.equals(accountId, that.accountId) && Objects.equals(reportDate, that.reportDate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(accountId, reportDate);
    }
}
