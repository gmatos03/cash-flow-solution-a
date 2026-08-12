package com.cashflow.ledger.reporting.repository;

import com.cashflow.ledger.reporting.entity.AccountEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountRepository extends JpaRepository<AccountEntity, String> {
}
