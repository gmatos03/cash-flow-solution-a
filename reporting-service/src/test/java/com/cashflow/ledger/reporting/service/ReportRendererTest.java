package com.cashflow.ledger.reporting.service;

import com.cashflow.ledger.reporting.domain.CashFlowAggregate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class ReportRendererTest {

    private final CashFlowAggregate sample = new CashFlowAggregate(
            "acc-10293847", LocalDate.of(2026, 8, 9),
            new BigDecimal("46963.55"), new BigDecimal("3200.00"),
            new BigDecimal("950.00"), new BigDecimal("49213.55"));

    @Test
    void renderCsv_producesHeaderAndOneDataRow() {
        ReportRenderer renderer = new ReportRenderer("./reports");

        String csv = renderer.renderCsv(sample);

        assertThat(csv).startsWith("account_id,report_date,opening_balance,total_credits,total_debits,closing_balance,reconciliation_status\n");
        assertThat(csv).contains("acc-10293847,2026-08-09,46963.55,3200.00,950.00,49213.55,PENDING");
    }

    @Test
    void renderPdf_producesNonEmptyPdfBytes() {
        ReportRenderer renderer = new ReportRenderer("./reports");

        byte[] pdf = renderer.renderPdf(sample);

        assertThat(pdf).isNotEmpty();
        assertThat(new String(pdf, 0, 5, java.nio.charset.StandardCharsets.US_ASCII)).isEqualTo("%PDF-");
    }

    @Test
    void writeToLocalStorage_writesBothFilesUnderAccountAndDate(@TempDir Path tempDir) throws Exception {
        ReportRenderer renderer = new ReportRenderer(tempDir.toString());

        Path dir = renderer.writeToLocalStorage(sample, renderer.renderCsv(sample), renderer.renderPdf(sample));

        assertThat(dir).isEqualTo(tempDir.resolve("acc-10293847").resolve("2026-08-09"));
        assertThat(Files.exists(dir.resolve("report.csv"))).isTrue();
        assertThat(Files.exists(dir.resolve("report.pdf"))).isTrue();
    }
}
