package com.cashflow.ledger.query.repository;

import com.cashflow.ledger.query.entity.DailyCashFlowLogEntity;
import com.cashflow.ledger.query.entity.DailyCashFlowLogId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DailyCashFlowLogRepository extends JpaRepository<DailyCashFlowLogEntity, DailyCashFlowLogId> {
}
