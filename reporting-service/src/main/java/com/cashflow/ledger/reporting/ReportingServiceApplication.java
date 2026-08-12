package com.cashflow.ledger.reporting;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Solution A - Reporting Service.
 * Builds the daily cash-flow log: aggregates ledger_entries, upserts
 * daily_cash_flow_log, and renders a CSV/PDF copy locally (in place of the
 * S3 report bucket used in Appendix D/F's AWS deployment). Runs on a
 * schedule and via an on-demand trigger endpoint.
 */
@SpringBootApplication
@EnableScheduling
public class ReportingServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ReportingServiceApplication.class, args);
    }
}
