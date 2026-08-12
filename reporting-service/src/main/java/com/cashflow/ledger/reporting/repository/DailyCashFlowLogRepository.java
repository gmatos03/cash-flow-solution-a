package com.cashflow.ledger.reporting.repository;

import com.cashflow.ledger.reporting.entity.DailyCashFlowLogEntity;
import com.cashflow.ledger.reporting.entity.DailyCashFlowLogId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface DailyCashFlowLogRepository extends JpaRepository<DailyCashFlowLogEntity, DailyCashFlowLogId> {

    /**
     * Idempotent upsert (Appendix F.7, step 3): re-running the job for a
     * date that was already reported simply recomputes and overwrites the
     * row rather than failing on a duplicate key.
     */
    @Modifying
    @Query(value = """
            INSERT INTO daily_cash_flow_log
                (account_id, report_date, opening_balance, total_credits, total_debits, closing_balance,
                 reconciliation_status, generated_at)
            VALUES (:accountId, :reportDate, :opening, :credits, :debits, :closing, 'PENDING', now())
            ON CONFLICT (account_id, report_date) DO UPDATE SET
                opening_balance = EXCLUDED.opening_balance,
                total_credits = EXCLUDED.total_credits,
                total_debits = EXCLUDED.total_debits,
                closing_balance = EXCLUDED.closing_balance,
                reconciliation_status = 'PENDING',
                generated_at = now()
            """, nativeQuery = true)
    void upsert(@Param("accountId") String accountId,
                @Param("reportDate") LocalDate reportDate,
                @Param("opening") BigDecimal opening,
                @Param("credits") BigDecimal credits,
                @Param("debits") BigDecimal debits,
                @Param("closing") BigDecimal closing);
}
