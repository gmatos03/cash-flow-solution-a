package com.cashflow.ledger.eventhandler.repository;

import com.cashflow.ledger.eventhandler.entity.AccountEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.Instant;

public interface AccountRepository extends JpaRepository<AccountEntity, String> {

    /**
     * Atomically adjusts the account's current_balance by {@code delta}
     * (positive for CREDIT, negative for DEBIT) directly in SQL, avoiding a
     * separate read-modify-write round trip and the race condition that
     * would come with it.
     *
     * @return the number of rows updated (0 if the account does not exist)
     */
    @Modifying
    @Query("UPDATE AccountEntity a SET a.currentBalance = a.currentBalance + :delta, a.updatedAt = :now " +
            "WHERE a.accountId = :accountId")
    int adjustBalance(@Param("accountId") String accountId, @Param("delta") BigDecimal delta, @Param("now") Instant now);
}
