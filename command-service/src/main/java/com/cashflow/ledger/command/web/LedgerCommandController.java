package com.cashflow.ledger.command.web;

import com.cashflow.ledger.command.domain.PostEntryCommand;
import com.cashflow.ledger.command.repository.EventStoreRepository;
import com.cashflow.ledger.command.service.PostEntryCommandHandler;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Exposes the write side under {@code /commands} (Appendix F.2).
 * Matches LedgerCommandController in Appendix B's C4 Level 3/4 diagrams.
 */
@RestController
@RequestMapping("/commands")
public class LedgerCommandController {

    private final PostEntryCommandHandler commandHandler;
    private final EventStoreRepository eventStoreRepository;

    public LedgerCommandController(PostEntryCommandHandler commandHandler,
                                    EventStoreRepository eventStoreRepository) {
        this.commandHandler = commandHandler;
        this.eventStoreRepository = eventStoreRepository;
    }

    @PostMapping("/entries")
    public ResponseEntity<EntryAcceptedResponse> postEntry(@Valid @RequestBody EntryRequest req) {
        PostEntryCommand cmd = new PostEntryCommand(
                null, // entryId is generated server-side
                req.accountId(),
                req.amount(),
                req.currency(),
                req.type(),
                req.channel(),
                req.description(),
                req.idempotencyKey()
        );
        EntryAcceptedResponse response = commandHandler.handle(cmd);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    @GetMapping("/entries/{entryId}/status")
    public ResponseEntity<EntryStatusResponse> getStatus(@PathVariable String entryId) {
        return eventStoreRepository.findByEntryId(entryId)
                .map(event -> ResponseEntity.ok(new EntryStatusResponse(entryId, "ACCEPTED", event.occurredAt())))
                .orElseGet(() -> ResponseEntity.notFound().build());
        // Note: a full implementation would also check the read model
        // (populated by the Event Handler Service) to distinguish ACCEPTED
        // from POSTED, as documented in Appendix F.2. The Command Service
        // alone can only confirm the event was appended.
    }
}
