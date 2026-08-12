package com.cashflow.ledger.reporting.service;

import com.cashflow.ledger.reporting.domain.CashFlowAggregate;
import com.cashflow.ledger.reporting.entity.AccountEntity;
import com.cashflow.ledger.reporting.repository.AccountRepository;
import com.cashflow.ledger.reporting.repository.LedgerEntryRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneOffset;

/**
 * Runs the aggregation query described in Appendix F.7 for a single
 * account/date: closing balance is reconstructed as of the end of
 * reportDate (account.openingBalance plus every credit/debit posted before
 * that cutoff), not read from the account's live current_balance - a report
 * for a fixed historical day must stay stable regardless of what posts on
 * later days. Opening balance is then derived by backing out the day's own
 * credits/debits from that reconstructed closing balance.
 */
@Service
public class CashFlowAggregationService {

    private final AccountRepository accountRepository;
    private final LedgerEntryRepository ledgerEntryRepository;

    public CashFlowAggregationService(AccountRepository accountRepository,
                                       LedgerEntryRepository ledgerEntryRepository) {
        this.accountRepository = accountRepository;
        this.ledgerEntryRepository = ledgerEntryRepository;
    }

    public CashFlowAggregate aggregate(String accountId, LocalDate reportDate) {
        AccountEntity account = accountRepository.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException(accountId));

        var start = reportDate.atStartOfDay(ZoneOffset.UTC).toInstant();
        var end = reportDate.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();

        BigDecimal totalCredits = ledgerEntryRepository.sumCredits(accountId, start, end);
        BigDecimal totalDebits = ledgerEntryRepository.sumDebits(accountId, start, end);

        BigDecimal creditsBeforeEnd = ledgerEntryRepository.sumCreditsBefore(accountId, end);
        BigDecimal debitsBeforeEnd = ledgerEntryRepository.sumDebitsBefore(accountId, end);
        BigDecimal closingBalance = account.getOpeningBalance().add(creditsBeforeEnd).subtract(debitsBeforeEnd);
        BigDecimal openingBalance = closingBalance.subtract(totalCredits).add(totalDebits);

        return new CashFlowAggregate(accountId, reportDate, openingBalance, totalCredits, totalDebits, closingBalance);
    }
}
