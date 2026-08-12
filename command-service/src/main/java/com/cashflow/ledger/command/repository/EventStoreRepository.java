package com.cashflow.ledger.command.repository;

import com.cashflow.ledger.command.domain.CreditDebitEvent;

import java.util.Optional;

/**
 * Port implemented by {@link JpaEventStoreRepository}. Kept as an interface
 * so the persistence technology (Aurora/PostgreSQL here, Cosmos DB in the
 * Azure version of Solution A) can vary without touching
 * {@link com.cashflow.ledger.command.service.PostEntryCommandHandler}.
 */
public interface EventStoreRepository {

    void append(CreditDebitEvent event);

    Optional<CreditDebitEvent> findByEntryId(String entryId);
}
