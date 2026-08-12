package com.cashflow.ledger.reporting.service;

import com.cashflow.ledger.reporting.domain.CashFlowAggregate;
import com.cashflow.ledger.reporting.entity.AccountEntity;
import com.cashflow.ledger.reporting.repository.AccountRepository;
import com.cashflow.ledger.reporting.repository.DailyCashFlowLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/**
 * Orchestrates the six-step reporting pipeline from Appendix F.7:
 * aggregate -> upsert -> render -> upload (local write) -> notify, run once
 * per account for a given business date.
 */
@Component
public class DailyCashFlowReportJob {

    private static final Logger log = LoggerFactory.getLogger(DailyCashFlowReportJob.class);

    private final AccountRepository accountRepository;
    private final DailyCashFlowLogRepository dailyCashFlowLogRepository;
    private final CashFlowAggregationService aggregationService;
    private final ReportRenderer reportRenderer;
    private final NotificationService notificationService;
    private final DailyCashFlowReportJob self;

    public DailyCashFlowReportJob(AccountRepository accountRepository,
                                   DailyCashFlowLogRepository dailyCashFlowLogRepository,
                                   CashFlowAggregationService aggregationService,
                                   ReportRenderer reportRenderer,
                                   NotificationService notificationService,
                                   @Lazy DailyCashFlowReportJob self) {
        this.accountRepository = accountRepository;
        this.dailyCashFlowLogRepository = dailyCashFlowLogRepository;
        this.aggregationService = aggregationService;
        this.reportRenderer = reportRenderer;
        this.notificationService = notificationService;
        this.self = self;
    }

    public int runForAllAccounts(LocalDate reportDate) {
        int processed = 0;
        // Must go through the proxy (self), not `this` - calling an @Transactional
        // method directly from within the same class bypasses Spring's AOP proxy,
        // silently skipping the transaction and breaking the @Modifying upsert below.
        DailyCashFlowReportJob proxy = self != null ? self : this;
        for (AccountEntity account : accountRepository.findAll()) {
            try {
                proxy.runForAccount(account.getAccountId(), reportDate);
                processed++;
            } catch (Exception ex) {
                log.error("Failed to build the daily cash-flow log for account {} on {}: {}",
                        account.getAccountId(), reportDate, ex.getMessage(), ex);
            }
        }
        log.info("Daily cash-flow log run complete for {}: {} account(s) processed", reportDate, processed);
        return processed;
    }

    @Transactional
    public CashFlowAggregate runForAccount(String accountId, LocalDate reportDate) {
        CashFlowAggregate agg = aggregationService.aggregate(accountId, reportDate);

        dailyCashFlowLogRepository.upsert(
                agg.accountId(), agg.reportDate(), agg.openingBalance(),
                agg.totalCredits(), agg.totalDebits(), agg.closingBalance());

        String csv = reportRenderer.renderCsv(agg);
        byte[] pdf = reportRenderer.renderPdf(agg);
        var location = reportRenderer.writeToLocalStorage(agg, csv, pdf);

        notificationService.notifyReportReady(agg, location);
        return agg;
    }
}
