package com.cashflow.ledger.command.service;

import com.cashflow.ledger.command.domain.CreditDebitEvent;
import com.cashflow.ledger.command.domain.LedgerAccountAggregate;
import com.cashflow.ledger.command.domain.PostEntryCommand;
import com.cashflow.ledger.command.kafka.EventPublisher;
import com.cashflow.ledger.command.repository.AccountEntity;
import com.cashflow.ledger.command.repository.AccountJpaRepository;
import com.cashflow.ledger.command.repository.EventStoreRepository;
import com.cashflow.ledger.command.web.EntryAcceptedResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Orchestrates validation, aggregate application, and persistence/publish
 * for a single command. Matches PostEntryCommandHandler in Appendix B's
 * C4 Level 3/4 diagrams for Solution A.
 */
@Service
public class PostEntryCommandHandler {

    private static final Logger log = LoggerFactory.getLogger(PostEntryCommandHandler.class);
    private static final Pattern ACCOUNT_ID_PATTERN = Pattern.compile("^acc-[A-Za-z0-9-]+$");

    private final EventStoreRepository eventStoreRepository;
    private final EventPublisher eventPublisher;
    private final IdempotencyService idempotencyService;
    private final AccountJpaRepository accountRepository;

    public PostEntryCommandHandler(EventStoreRepository eventStoreRepository,
                                    EventPublisher eventPublisher,
                                    IdempotencyService idempotencyService,
                                    AccountJpaRepository accountRepository) {
        this.eventStoreRepository = eventStoreRepository;
        this.eventPublisher = eventPublisher;
        this.idempotencyService = idempotencyService;
        this.accountRepository = accountRepository;
    }

    public EntryAcceptedResponse handle(PostEntryCommand cmdWithoutId) {
        String entryId = "ent-" + UUID.randomUUID().toString().substring(0, 8);
        PostEntryCommand cmd = new PostEntryCommand(
                entryId,
                cmdWithoutId.accountId(),
                cmdWithoutId.amount(),
                cmdWithoutId.currency(),
                cmdWithoutId.type(),
                cmdWithoutId.channel(),
                cmdWithoutId.description(),
                cmdWithoutId.idempotencyKey()
        );

        if (!ACCOUNT_ID_PATTERN.matcher(cmd.accountId()).matches()) {
            throw new InvalidAccountException("accountId '" + cmd.accountId() + "' is not a recognized account");
        }

        AccountEntity account = accountRepository.findById(cmd.accountId())
                .orElseThrow(() -> new InvalidAccountException(
                        "accountId '" + cmd.accountId() + "' is not a recognized account"));
        if (!account.getCurrency().equals(cmd.currency())) {
            throw new CurrencyMismatchException(
                    "entry currency '" + cmd.currency() + "' does not match account '" + cmd.accountId() +
                            "'s ledger currency '" + account.getCurrency() + "'");
        }

        Optional<String> existingEntryId = idempotencyService.reserve(cmd.idempotencyKey(), entryId);
        if (existingEntryId.isPresent()) {
            throw new IdempotencyConflictException(existingEntryId.get());
        }

        CreditDebitEvent event = LedgerAccountAggregate.apply(cmd);
        eventStoreRepository.append(event);
        eventPublisher.publish(event);

        log.info("Accepted entry {} ({} {} {}) for account {}",
                entryId, cmd.type(), cmd.amount(), cmd.currency(), cmd.accountId());

        return new EntryAcceptedResponse(entryId, cmd.accountId(), event.eventId(), "ACCEPTED", event.occurredAt());
    }
}
