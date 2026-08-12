package com.cashflow.ledger.query.repository;

import com.cashflow.ledger.query.entity.AccountEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountRepository extends JpaRepository<AccountEntity, String> {
}
