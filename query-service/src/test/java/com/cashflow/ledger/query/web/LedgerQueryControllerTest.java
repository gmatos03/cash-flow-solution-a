package com.cashflow.ledger.query.web;

import com.cashflow.ledger.query.service.AccountNotFoundException;
import com.cashflow.ledger.query.service.BalanceQueryService;
import com.cashflow.ledger.query.service.StatementQueryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.Instant;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class LedgerQueryControllerTest {

    @Mock
    private BalanceQueryService balanceQueryService;

    @Mock
    private StatementQueryService statementQueryService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        LedgerQueryController controller = new LedgerQueryController(balanceQueryService, statementQueryService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void getBalance_returns200_withBalancePayload() throws Exception {
        when(balanceQueryService.getBalance("acc-10293847")).thenReturn(
                new BalanceResponse("acc-10293847", new BigDecimal("48213.55"), "USD", Instant.now(), "cache"));

        mockMvc.perform(get("/queries/accounts/acc-10293847/balance"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountId").value("acc-10293847"))
                .andExpect(jsonPath("$.currentBalance").value(48213.55));
    }

    @Test
    void getBalance_returns404_whenAccountDoesNotExist() throws Exception {
        when(balanceQueryService.getBalance("acc-missing"))
                .thenThrow(new AccountNotFoundException("acc-missing"));

        mockMvc.perform(get("/queries/accounts/acc-missing/balance"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("NOT_FOUND"));
    }
}
