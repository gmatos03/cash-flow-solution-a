package com.cashflow.ledger.command.repository;

import com.cashflow.ledger.command.domain.CreditDebitEvent;
import com.cashflow.ledger.command.domain.EntryType;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class JpaEventStoreRepository implements EventStoreRepository {

    private final EventStoreJpaRepository jpaRepository;

    public JpaEventStoreRepository(EventStoreJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public void append(CreditDebitEvent event) {
        EventStoreEntity entity = new EventStoreEntity(
                event.eventId(),
                event.entryId(),
                event.accountId(),
                event.type().name(),
                event.amount(),
                event.currency(),
                event.channel(),
                event.description(),
                event.occurredAt(),
                event.schemaVersion()
        );
        jpaRepository.save(entity);
    }

    @Override
    public Optional<CreditDebitEvent> findByEntryId(String entryId) {
        return jpaRepository.findFirstByEntryId(entryId).map(e -> new CreditDebitEvent(
                e.getEventId(),
                e.getEntryId(),
                e.getAccountId(),
                EntryType.valueOf(e.getType()),
                e.getAmount(),
                e.getCurrency(),
                e.getChannel(),
                e.getDescription(),
                e.getOccurredAt(),
                e.getSchemaVersion()
        ));
    }
}
