package com.cashflow.ledger.command.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/** Spring Data JPA repository backing {@link JpaEventStoreRepository}. */
public interface EventStoreJpaRepository extends JpaRepository<EventStoreEntity, UUID> {

    Optional<EventStoreEntity> findFirstByEntryId(String entryId);
}
