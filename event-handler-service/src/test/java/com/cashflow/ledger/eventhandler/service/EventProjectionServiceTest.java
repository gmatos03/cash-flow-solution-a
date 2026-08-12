package com.cashflow.ledger.eventhandler.service;

import com.cashflow.ledger.eventhandler.cache.BalanceCacheEvictor;
import com.cashflow.ledger.eventhandler.cache.StatementCacheEvictor;
import com.cashflow.ledger.eventhandler.domain.CreditDebitEvent;
import com.cashflow.ledger.eventhandler.domain.EntryType;
import com.cashflow.ledger.eventhandler.entity.LedgerEntryEntity;
import com.cashflow.ledger.eventhandler.repository.AccountRepository;
import com.cashflow.ledger.eventhandler.repository.LedgerEntryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventProjectionServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private LedgerEntryRepository ledgerEntryRepository;

    @Mock
    private BalanceCacheEvictor balanceCacheEvictor;

    @Mock
    private StatementCacheEvictor statementCacheEvictor;

    @InjectMocks
    private EventProjectionService projectionService;

    private CreditDebitEvent buildEvent(EntryType type, BigDecimal amount) {
        return new CreditDebitEvent(
                UUID.randomUUID(), "ent-1", "acc-10293847", type, amount, "USD",
                "WEB", "test", Instant.now(), 1);
    }

    @Test
    void project_appliesCreditAndInvalidatesCache_whenAccountExists() {
        CreditDebitEvent event = buildEvent(EntryType.CREDIT, new BigDecimal("1250.00"));
        when(ledgerEntryRepository.existsById("ent-1")).thenReturn(false);
        when(accountRepository.adjustBalance(eq("acc-10293847"), eq(new BigDecimal("1250.00")), any())).thenReturn(1);

        projectionService.project(event);

        ArgumentCaptor<LedgerEntryEntity> captor = ArgumentCaptor.forClass(LedgerEntryEntity.class);
        verify(ledgerEntryRepository, times(1)).save(captor.capture());
        assertThat(captor.getValue().getEntryId()).isEqualTo("ent-1");
        verify(balanceCacheEvictor, times(1)).evict("acc-10293847");
        verify(statementCacheEvictor, times(1)).evict("acc-10293847");
    }

    @Test
    void project_negatesAmountForDebit() {
        CreditDebitEvent event = buildEvent(EntryType.DEBIT, new BigDecimal("300.00"));
        when(ledgerEntryRepository.existsById("ent-1")).thenReturn(false);
        when(accountRepository.adjustBalance(eq("acc-10293847"), eq(new BigDecimal("-300.00")), any())).thenReturn(1);

        projectionService.project(event);

        verify(accountRepository, times(1)).adjustBalance(eq("acc-10293847"), eq(new BigDecimal("-300.00")), any());
    }

    @Test
    void project_isIdempotent_whenEntryAlreadyProjected() {
        CreditDebitEvent event = buildEvent(EntryType.CREDIT, new BigDecimal("1250.00"));
        when(ledgerEntryRepository.existsById("ent-1")).thenReturn(true);

        projectionService.project(event);

        verify(accountRepository, never()).adjustBalance(anyString(), any(), any());
        verify(ledgerEntryRepository, never()).save(any());
        verify(balanceCacheEvictor, never()).evict(anyString());
        verify(statementCacheEvictor, never()).evict(anyString());
    }

    @Test
    void project_skipsSilently_whenAccountDoesNotExist() {
        CreditDebitEvent event = buildEvent(EntryType.CREDIT, new BigDecimal("50.00"));
        when(ledgerEntryRepository.existsById("ent-1")).thenReturn(false);
        when(accountRepository.adjustBalance(anyString(), any(), any())).thenReturn(0);

        projectionService.project(event);

        verify(ledgerEntryRepository, never()).save(any());
        verify(balanceCacheEvictor, never()).evict(anyString());
        verify(statementCacheEvictor, never()).evict(anyString());
    }
}
