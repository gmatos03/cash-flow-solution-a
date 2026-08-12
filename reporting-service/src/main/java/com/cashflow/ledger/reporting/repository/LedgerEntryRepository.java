package com.cashflow.ledger.reporting.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Read-only projection queries over ledger_entries used by
 * CashFlowAggregationService; there is no owning entity here on purpose -
 * the Reporting Service never writes to this table.
 */
@Repository
public interface LedgerEntryRepository extends JpaRepository<com.cashflow.ledger.reporting.entity.LedgerEntryEntity, String> {

    @Query("SELECT COALESCE(SUM(e.amount), 0) FROM LedgerEntryEntity e " +
            "WHERE e.accountId = :accountId AND e.type = 'CREDIT' AND e.postedAt >= :start AND e.postedAt < :end")
    BigDecimal sumCredits(@Param("accountId") String accountId, @Param("start") Instant start, @Param("end") Instant end);

    @Query("SELECT COALESCE(SUM(e.amount), 0) FROM LedgerEntryEntity e " +
            "WHERE e.accountId = :accountId AND e.type = 'DEBIT' AND e.postedAt >= :start AND e.postedAt < :end")
    BigDecimal sumDebits(@Param("accountId") String accountId, @Param("start") Instant start, @Param("end") Instant end);

    /**
     * Cumulative totals from the account's inception up to (but not
     * including) {@code end}, used to reconstruct the balance as of a fixed
     * point in time rather than reading the account's live current_balance
     * - see CashFlowAggregationService.
     */
    @Query("SELECT COALESCE(SUM(e.amount), 0) FROM LedgerEntryEntity e " +
            "WHERE e.accountId = :accountId AND e.type = 'CREDIT' AND e.postedAt < :end")
    BigDecimal sumCreditsBefore(@Param("accountId") String accountId, @Param("end") Instant end);

    @Query("SELECT COALESCE(SUM(e.amount), 0) FROM LedgerEntryEntity e " +
            "WHERE e.accountId = :accountId AND e.type = 'DEBIT' AND e.postedAt < :end")
    BigDecimal sumDebitsBefore(@Param("accountId") String accountId, @Param("end") Instant end);
}
