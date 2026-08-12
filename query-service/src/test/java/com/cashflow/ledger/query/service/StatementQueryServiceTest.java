package com.cashflow.ledger.query.service;

import com.cashflow.ledger.query.cache.StatementCacheService;
import com.cashflow.ledger.query.entity.DailyCashFlowLogEntity;
import com.cashflow.ledger.query.entity.DailyCashFlowLogId;
import com.cashflow.ledger.query.entity.LedgerEntryEntity;
import com.cashflow.ledger.query.repository.DailyCashFlowLogRepository;
import com.cashflow.ledger.query.repository.LedgerEntryRepository;
import com.cashflow.ledger.query.web.DailyCashFlowLogResponse;
import com.cashflow.ledger.query.web.StatementResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StatementQueryServiceTest {

    @Mock
    private LedgerEntryRepository ledgerEntryRepository;

    @Mock
    private DailyCashFlowLogRepository dailyCashFlowLogRepository;

    @Mock
    private StatementCacheService statementCacheService;

    @InjectMocks
    private StatementQueryService statementQueryService;

    @Test
    void getStatement_queriesRepositoryAndCaches_whenCacheMiss() throws Exception {
        LocalDate from = LocalDate.of(2026, 8, 1);
        LocalDate to = LocalDate.of(2026, 8, 10);
        when(statementCacheService.buildKey("acc-1", from, to, 0, 20)).thenReturn("statement:acc-1:...");
        when(statementCacheService.get("statement:acc-1:...")).thenReturn(Optional.empty());

        LedgerEntryEntity entry = buildEntry("ent-1", "acc-1", "CREDIT", new BigDecimal("100.00"));
        Page<LedgerEntryEntity> page = new PageImpl<>(List.of(entry), PageRequest.of(0, 20), 1);
        when(ledgerEntryRepository.findByAccountIdAndPostedAtBetweenOrderByPostedAtDesc(
                any(), any(), any(), any())).thenReturn(page);

        StatementResponse response = statementQueryService.getStatement("acc-1", from, to, 0, 20);

        assertThat(response.entries()).hasSize(1);
        assertThat(response.entries().get(0).entryId()).isEqualTo("ent-1");
        assertThat(response.totalElements()).isEqualTo(1);
        verify(statementCacheService).put(anyString(), any());
    }

    @Test
    void getStatement_doesNotCache_whenResultIsEmpty() {
        // A read racing a concurrent projection can observe "no entries
        // yet"; caching that would leave a stale empty result stuck for the
        // full TTL after the projection's own cache eviction already ran
        // (see EventProjectionService/StatementCacheEvictor). Empty pages
        // must never be cached.
        LocalDate from = LocalDate.of(2026, 8, 1);
        LocalDate to = LocalDate.of(2026, 8, 10);
        when(statementCacheService.buildKey("acc-1", from, to, 0, 20)).thenReturn("statement:acc-1:...");
        when(statementCacheService.get("statement:acc-1:...")).thenReturn(Optional.empty());

        Page<LedgerEntryEntity> emptyPage = new PageImpl<>(List.of(), PageRequest.of(0, 20), 0);
        when(ledgerEntryRepository.findByAccountIdAndPostedAtBetweenOrderByPostedAtDesc(
                any(), any(), any(), any())).thenReturn(emptyPage);

        StatementResponse response = statementQueryService.getStatement("acc-1", from, to, 0, 20);

        assertThat(response.entries()).isEmpty();
        verify(statementCacheService, never()).put(anyString(), any());
    }

    @Test
    void getDailyLog_returnsMappedResponse_whenFound() throws Exception {
        LocalDate reportDate = LocalDate.of(2026, 8, 9);
        DailyCashFlowLogEntity entity = buildLog("acc-1", reportDate);
        when(dailyCashFlowLogRepository.findById(new DailyCashFlowLogId("acc-1", reportDate)))
                .thenReturn(Optional.of(entity));

        DailyCashFlowLogResponse response = statementQueryService.getDailyLog("acc-1", reportDate);

        assertThat(response.reconciliationStatus()).isEqualTo("RECONCILED");
        assertThat(response.closingBalance()).isEqualByComparingTo("49213.55");
    }

    @Test
    void getDailyLog_throwsReportNotFound_whenMissing() {
        LocalDate reportDate = LocalDate.of(2026, 8, 9);
        when(dailyCashFlowLogRepository.findById(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> statementQueryService.getDailyLog("acc-1", reportDate))
                .isInstanceOf(ReportNotFoundException.class);
    }

    private LedgerEntryEntity buildEntry(String entryId, String accountId, String type, BigDecimal amount) throws Exception {
        LedgerEntryEntity entry = new LedgerEntryEntity();
        setField(entry, "entryId", entryId);
        setField(entry, "accountId", accountId);
        setField(entry, "type", type);
        setField(entry, "amount", amount);
        setField(entry, "currency", "USD");
        setField(entry, "channel", "WEB");
        setField(entry, "description", "test entry");
        setField(entry, "postedAt", Instant.now());
        setField(entry, "status", "POSTED");
        return entry;
    }

    private DailyCashFlowLogEntity buildLog(String accountId, LocalDate reportDate) throws Exception {
        DailyCashFlowLogEntity entity = new DailyCashFlowLogEntity();
        setField(entity, "accountId", accountId);
        setField(entity, "reportDate", reportDate);
        setField(entity, "openingBalance", new BigDecimal("46963.55"));
        setField(entity, "totalCredits", new BigDecimal("3200.00"));
        setField(entity, "totalDebits", new BigDecimal("950.00"));
        setField(entity, "closingBalance", new BigDecimal("49213.55"));
        setField(entity, "reconciliationStatus", "RECONCILED");
        setField(entity, "generatedAt", Instant.now());
        return entity;
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
