package com.cashflow.ledger.query.web;

import com.cashflow.ledger.query.service.BalanceQueryService;
import com.cashflow.ledger.query.service.StatementQueryService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/**
 * Exposes the read side under {@code /queries} (Appendix F.3).
 * Matches LedgerQueryController referenced in Appendix F.
 */
@RestController
@RequestMapping("/queries")
public class LedgerQueryController {

    private final BalanceQueryService balanceQueryService;
    private final StatementQueryService statementQueryService;

    public LedgerQueryController(BalanceQueryService balanceQueryService,
                                  StatementQueryService statementQueryService) {
        this.balanceQueryService = balanceQueryService;
        this.statementQueryService = statementQueryService;
    }

    @GetMapping("/accounts/{accountId}/balance")
    public ResponseEntity<BalanceResponse> getBalance(@PathVariable String accountId) {
        return ResponseEntity.ok(balanceQueryService.getBalance(accountId));
    }

    @GetMapping("/accounts/{accountId}/statement")
    public ResponseEntity<StatementResponse> getStatement(
            @PathVariable String accountId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(statementQueryService.getStatement(accountId, from, to, page, size));
    }

    @GetMapping("/accounts/{accountId}/daily-log/{date}")
    public ResponseEntity<DailyCashFlowLogResponse> getDailyLog(
            @PathVariable String accountId,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(statementQueryService.getDailyLog(accountId, date));
    }
}
