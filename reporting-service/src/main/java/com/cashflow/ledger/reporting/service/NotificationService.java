package com.cashflow.ledger.reporting.service;

import com.cashflow.ledger.reporting.domain.CashFlowAggregate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.nio.file.Path;

/**
 * Stands in for the Amazon SNS/SES notification described in Appendix F.7
 * step 6. On AWS this would publish a summary event with a pre-signed S3
 * link; locally it just logs where the report was written.
 */
@Component
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    public void notifyReportReady(CashFlowAggregate agg, Path location) {
        log.info("[notification] Daily cash-flow log for {} on {} is ready at {}",
                agg.accountId(), agg.reportDate(), location.toAbsolutePath());
    }
}
