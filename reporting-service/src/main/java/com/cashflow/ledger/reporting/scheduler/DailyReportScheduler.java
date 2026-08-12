package com.cashflow.ledger.reporting.scheduler;

import com.cashflow.ledger.reporting.service.DailyCashFlowReportJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneOffset;

/**
 * Local stand-in for the EventBridge Scheduler cron rule in Appendix F.7.
 * Cron expression is externalized to app.reporting.cron (application.yml).
 */
@Component
public class DailyReportScheduler {

    private static final Logger log = LoggerFactory.getLogger(DailyReportScheduler.class);

    private final DailyCashFlowReportJob reportJob;

    public DailyReportScheduler(DailyCashFlowReportJob reportJob) {
        this.reportJob = reportJob;
    }

    @Scheduled(cron = "${app.reporting.cron}")
    public void runDailyReport() {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        log.info("Scheduled daily cash-flow log run starting for {}", today);
        reportJob.runForAllAccounts(today);
    }
}
