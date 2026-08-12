package com.cashflow.ledger.query.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "daily_cash_flow_log")
@IdClass(DailyCashFlowLogId.class)
public class DailyCashFlowLogEntity {

    @Id
    @Column(name = "account_id")
    private String accountId;

    @Id
    @Column(name = "report_date")
    private LocalDate reportDate;

    @Column(name = "opening_balance", nullable = false)
    private BigDecimal openingBalance;

    @Column(name = "total_credits", nullable = false)
    private BigDecimal totalCredits;

    @Column(name = "total_debits", nullable = false)
    private BigDecimal totalDebits;

    @Column(name = "closing_balance", nullable = false)
    private BigDecimal closingBalance;

    @Column(name = "reconciliation_status", nullable = false)
    private String reconciliationStatus;

    @Column(name = "generated_at", nullable = false)
    private Instant generatedAt;

    public DailyCashFlowLogEntity() {
        // JPA
    }

    public String getAccountId() {
        return accountId;
    }

    public LocalDate getReportDate() {
        return reportDate;
    }

    public BigDecimal getOpeningBalance() {
        return openingBalance;
    }

    public BigDecimal getTotalCredits() {
        return totalCredits;
    }

    public BigDecimal getTotalDebits() {
        return totalDebits;
    }

    public BigDecimal getClosingBalance() {
        return closingBalance;
    }

    public String getReconciliationStatus() {
        return reconciliationStatus;
    }

    public Instant getGeneratedAt() {
        return generatedAt;
    }
}
