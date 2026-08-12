package com.cashflow.ledger.eventhandler.repository;

import com.cashflow.ledger.eventhandler.entity.LedgerEntryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LedgerEntryRepository extends JpaRepository<LedgerEntryEntity, String> {
}
