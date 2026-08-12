package com.cashflow.ledger.query.service;

import com.cashflow.ledger.query.cache.StatementCacheService;
import com.cashflow.ledger.query.entity.DailyCashFlowLogEntity;
import com.cashflow.ledger.query.entity.DailyCashFlowLogId;
import com.cashflow.ledger.query.entity.LedgerEntryEntity;
import com.cashflow.ledger.query.repository.DailyCashFlowLogRepository;
import com.cashflow.ledger.query.repository.LedgerEntryRepository;
import com.cashflow.ledger.query.web.DailyCashFlowLogResponse;
import com.cashflow.ledger.query.web.StatementEntryDto;
import com.cashflow.ledger.query.web.StatementResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;

/**
 * Backs {@code GET /queries/accounts/{accountId}/statement} and
 * {@code GET /queries/accounts/{accountId}/daily-log/{date}} (Appendix F.3).
 */
@Service
public class StatementQueryService {

    private final LedgerEntryRepository ledgerEntryRepository;
    private final DailyCashFlowLogRepository dailyCashFlowLogRepository;
    private final StatementCacheService statementCacheService;

    public StatementQueryService(LedgerEntryRepository ledgerEntryRepository,
                                  DailyCashFlowLogRepository dailyCashFlowLogRepository,
                                  StatementCacheService statementCacheService) {
        this.ledgerEntryRepository = ledgerEntryRepository;
        this.dailyCashFlowLogRepository = dailyCashFlowLogRepository;
        this.statementCacheService = statementCacheService;
    }

    public StatementResponse getStatement(String accountId, LocalDate from, LocalDate to, int page, int size) {
        String cacheKey = statementCacheService.buildKey(accountId, from, to, page, size);
        Optional<StatementResponse> cached = statementCacheService.get(cacheKey);
        if (cached.isPresent()) {
            return cached.get();
        }

        Page<LedgerEntryEntity> result = ledgerEntryRepository.findByAccountIdAndPostedAtBetweenOrderByPostedAtDesc(
                accountId,
                from.atStartOfDay(ZoneOffset.UTC).toInstant(),
                to.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant(),
                PageRequest.of(page, size));

        var entries = result.getContent().stream()
                .map(e -> new StatementEntryDto(
                        e.getEntryId(), e.getType(), e.getAmount(), e.getCurrency(), e.getPostedAt(), e.getDescription()))
                .toList();

        StatementResponse response = new StatementResponse(accountId, entries, page, size, result.getTotalElements());
        if (result.getTotalElements() > 0) {
            // Don't cache empty results: a read racing a concurrent
            // projection can observe "no entries yet" and cache that right
            // after the projection's own cache eviction already ran,
            // leaving the empty result stuck for the full TTL with nothing
            // left to evict it. Empty pages are cheap to recompute, so the
            // safe default is to not cache them at all.
            statementCacheService.put(cacheKey, response);
        }
        return response;
    }

    public DailyCashFlowLogResponse getDailyLog(String accountId, LocalDate reportDate) {
        DailyCashFlowLogEntity entity = dailyCashFlowLogRepository.findById(new DailyCashFlowLogId(accountId, reportDate))
                .orElseThrow(() -> new ReportNotFoundException(accountId, reportDate));

        return new DailyCashFlowLogResponse(
                entity.getAccountId(),
                entity.getReportDate(),
                entity.getOpeningBalance(),
                entity.getTotalCredits(),
                entity.getTotalDebits(),
                entity.getClosingBalance(),
                entity.getReconciliationStatus()
        );
    }
}
