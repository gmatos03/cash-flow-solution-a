package com.cashflow.ledger.command.web;

import com.cashflow.ledger.command.domain.PostEntryCommand;
import com.cashflow.ledger.command.repository.EventStoreRepository;
import com.cashflow.ledger.command.service.IdempotencyConflictException;
import com.cashflow.ledger.command.service.PostEntryCommandHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class LedgerCommandControllerTest {

    @Mock
    private PostEntryCommandHandler commandHandler;

    @Mock
    private EventStoreRepository eventStoreRepository;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @BeforeEach
    void setUp() {
        LedgerCommandController controller = new LedgerCommandController(commandHandler, eventStoreRepository);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void postEntry_returns202Accepted_whenRequestIsValid() throws Exception {
        when(commandHandler.handle(any(PostEntryCommand.class))).thenReturn(
                new EntryAcceptedResponse("ent-7a1c9e02", "acc-10293847", UUID.randomUUID(), "ACCEPTED", Instant.now()));

        String requestJson = """
                {
                  "accountId": "acc-10293847",
                  "amount": 1250.00,
                  "currency": "USD",
                  "type": "CREDIT",
                  "channel": "WEB",
                  "description": "Wire transfer received",
                  "idempotencyKey": "5f2c1a3e-9b7d-4e21-8c4a-2d6f9b1e7a44"
                }
                """;

        mockMvc.perform(post("/commands/entries")
                        .contentType("application/json")
                        .content(requestJson))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.entryId").value("ent-7a1c9e02"))
                .andExpect(jsonPath("$.status").value("ACCEPTED"));
    }

    @Test
    void postEntry_returns400_whenAmountIsMissing() throws Exception {
        String requestJson = """
                {
                  "accountId": "acc-10293847",
                  "currency": "USD",
                  "type": "CREDIT",
                  "channel": "WEB",
                  "idempotencyKey": "5f2c1a3e-9b7d-4e21-8c4a-2d6f9b1e7a44"
                }
                """;

        mockMvc.perform(post("/commands/entries")
                        .contentType("application/json")
                        .content(requestJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test
    void postEntry_returns409_whenIdempotencyKeyReused() throws Exception {
        when(commandHandler.handle(any(PostEntryCommand.class)))
                .thenThrow(new IdempotencyConflictException("ent-original1"));

        String requestJson = """
                {
                  "accountId": "acc-10293847",
                  "amount": 1250.00,
                  "currency": "USD",
                  "type": "CREDIT",
                  "channel": "WEB",
                  "idempotencyKey": "5f2c1a3e-9b7d-4e21-8c4a-2d6f9b1e7a44"
                }
                """;

        mockMvc.perform(post("/commands/entries")
                        .contentType("application/json")
                        .content(requestJson))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.entryId").value("ent-original1"));
    }
}
