package com.cashflow.ledger.eventhandler.service;

import com.cashflow.ledger.eventhandler.cache.BalanceCacheEvictor;
import com.cashflow.ledger.eventhandler.cache.StatementCacheEvictor;
import com.cashflow.ledger.eventhandler.domain.CreditDebitEvent;
import com.cashflow.ledger.eventhandler.entity.LedgerEntryEntity;
import com.cashflow.ledger.eventhandler.repository.AccountRepository;
import com.cashflow.ledger.eventhandler.repository.LedgerEntryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Projects a CreditDebitEvent into the read model: updates the account's
 * running balance and inserts the corresponding ledger_entries row, then
 * evicts the stale balance cache entry (Appendix F.6 / F.8).
 */
@Service
public class EventProjectionService {

    private static final Logger log = LoggerFactory.getLogger(EventProjectionService.class);

    private final AccountRepository accountRepository;
    private final LedgerEntryRepository ledgerEntryRepository;
    private final BalanceCacheEvictor balanceCacheEvictor;
    private final StatementCacheEvictor statementCacheEvictor;

    public EventProjectionService(AccountRepository accountRepository,
                                   LedgerEntryRepository ledgerEntryRepository,
                                   BalanceCacheEvictor balanceCacheEvictor,
                                   StatementCacheEvictor statementCacheEvictor) {
        this.accountRepository = accountRepository;
        this.ledgerEntryRepository = ledgerEntryRepository;
        this.balanceCacheEvictor = balanceCacheEvictor;
        this.statementCacheEvictor = statementCacheEvictor;
    }

    @Transactional
    public void project(CreditDebitEvent event) {
        if (ledgerEntryRepository.existsById(event.entryId())) {
            // Kafka gives at-least-once delivery; this makes projection
            // idempotent against redelivery of the same entry.
            log.info("Entry {} was already projected; skipping redelivered event {}",
                    event.entryId(), event.eventId());
            return;
        }

        BigDecimal delta = switch (event.type()) {
            case CREDIT -> event.amount();
            case DEBIT -> event.amount().negate();
        };

        int updated = accountRepository.adjustBalance(event.accountId(), delta, Instant.now());
        if (updated == 0) {
            log.warn("Received event {} for unknown account {}; skipping projection",
                    event.eventId(), event.accountId());
            return;
        }

        LedgerEntryEntity entry = new LedgerEntryEntity(
                event.entryId(),
                event.accountId(),
                event.type().name(),
                event.amount(),
                event.currency(),
                event.channel(),
                event.description(),
                event.occurredAt(),
                "POSTED"
        );
        ledgerEntryRepository.save(entry);
        balanceCacheEvictor.evict(event.accountId());
        statementCacheEvictor.evict(event.accountId());

        log.info("Projected entry {} ({} {} {}) for account {}",
                event.entryId(), event.type(), event.amount(), event.currency(), event.accountId());
    }
}
