package com.cashflow.ledger.query.repository;

import com.cashflow.ledger.query.entity.LedgerEntryEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;

public interface LedgerEntryRepository extends JpaRepository<LedgerEntryEntity, String> {

    Page<LedgerEntryEntity> findByAccountIdAndPostedAtBetweenOrderByPostedAtDesc(
            String accountId, Instant from, Instant to, Pageable pageable);
}
