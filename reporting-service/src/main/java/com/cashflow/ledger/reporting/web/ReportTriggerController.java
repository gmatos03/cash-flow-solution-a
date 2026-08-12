package com.cashflow.ledger.reporting.web;

import com.cashflow.ledger.reporting.service.DailyCashFlowReportJob;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

/**
 * On-demand equivalent of the scheduled run, useful for local testing
 * without waiting for the cron trigger (Appendix F.7).
 */
@RestController
public class ReportTriggerController {

    private final DailyCashFlowReportJob reportJob;

    public ReportTriggerController(DailyCashFlowReportJob reportJob) {
        this.reportJob = reportJob;
    }

    @PostMapping("/reports/run")
    public ResponseEntity<ReportRunResponse> run(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        LocalDate reportDate = date != null ? date : LocalDate.now(ZoneOffset.UTC);
        int processed = reportJob.runForAllAccounts(reportDate);
        return ResponseEntity.ok(new ReportRunResponse(reportDate, processed, Instant.now()));
    }
}
