package com.cashflow.ledger.reporting.service;

import com.cashflow.ledger.reporting.domain.CashFlowAggregate;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Renders a CashFlowAggregate as CSV and PDF (Appendix F.7, step 4) and
 * writes both under {@code app.reporting.output-dir}, mirroring the
 * reports/{accountId}/{reportDate}/ layout used for the S3 report bucket
 * in the AWS deployment.
 */
@Component
public class ReportRenderer {

    private final Path outputDir;

    public ReportRenderer(@Value("${app.reporting.output-dir}") String outputDir) {
        this.outputDir = Path.of(outputDir);
    }

    public String renderCsv(CashFlowAggregate agg) {
        StringBuilder sb = new StringBuilder();
        sb.append("account_id,report_date,opening_balance,total_credits,total_debits,closing_balance,reconciliation_status\n");
        sb.append(agg.accountId()).append(',')
                .append(agg.reportDate()).append(',')
                .append(agg.openingBalance()).append(',')
                .append(agg.totalCredits()).append(',')
                .append(agg.totalDebits()).append(',')
                .append(agg.closingBalance()).append(',')
                .append("PENDING\n");
        return sb.toString();
    }

    public byte[] renderPdf(CashFlowAggregate agg) {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);

            try (PDPageContentStream cs = new PDPageContentStream(document, page)) {
                cs.beginText();
                cs.setFont(PDType1Font.HELVETICA_BOLD, 16);
                cs.newLineAtOffset(50, 760);
                cs.showText("Daily Cash-Flow Log");
                cs.endText();

                String[] lines = new String[] {
                        "Account:          " + agg.accountId(),
                        "Report date:      " + agg.reportDate(),
                        "Opening balance:  " + agg.openingBalance(),
                        "Total credits:    " + agg.totalCredits(),
                        "Total debits:     " + agg.totalDebits(),
                        "Closing balance:  " + agg.closingBalance(),
                        "Status:           PENDING"
                };

                cs.beginText();
                cs.setFont(PDType1Font.HELVETICA, 11);
                cs.newLineAtOffset(50, 725);
                float leading = 18f;
                for (String line : lines) {
                    cs.showText(line);
                    cs.newLineAtOffset(0, -leading);
                }
                cs.endText();
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            document.save(out);
            return out.toByteArray();
        } catch (IOException ex) {
            throw new ReportRenderingException("Failed to render PDF for " + agg.accountId(), ex);
        }
    }

    public Path writeToLocalStorage(CashFlowAggregate agg, String csv, byte[] pdf) {
        try {
            Path dir = outputDir.resolve(agg.accountId()).resolve(agg.reportDate().toString());
            Files.createDirectories(dir);
            Files.write(dir.resolve("report.csv"), csv.getBytes(StandardCharsets.UTF_8));
            Files.write(dir.resolve("report.pdf"), pdf);
            return dir;
        } catch (IOException ex) {
            throw new ReportRenderingException("Failed to write report files for " + agg.accountId(), ex);
        }
    }
}
