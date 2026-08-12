package com.cashflow.ledger.reporting.service;

import com.cashflow.ledger.reporting.domain.CashFlowAggregate;
import com.cashflow.ledger.reporting.entity.AccountEntity;
import com.cashflow.ledger.reporting.repository.AccountRepository;
import com.cashflow.ledger.reporting.repository.LedgerEntryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CashFlowAggregationServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private LedgerEntryRepository ledgerEntryRepository;

    @InjectMocks
    private CashFlowAggregationService aggregationService;

    @Test
    void aggregate_computesOpeningBalanceFromClosingBalanceAndDayTotals() throws Exception {
        AccountEntity account = new AccountEntity();
        setField(account, "accountId", "acc-10293847");
        setField(account, "openingBalance", new BigDecimal("40000.00"));
        setField(account, "currentBalance", new BigDecimal("49213.55"));
        when(accountRepository.findById("acc-10293847")).thenReturn(Optional.of(account));
        when(ledgerEntryRepository.sumCredits(any(), any(), any())).thenReturn(new BigDecimal("3200.00"));
        when(ledgerEntryRepository.sumDebits(any(), any(), any())).thenReturn(new BigDecimal("950.00"));
        when(ledgerEntryRepository.sumCreditsBefore(any(), any())).thenReturn(new BigDecimal("10163.55"));
        when(ledgerEntryRepository.sumDebitsBefore(any(), any())).thenReturn(new BigDecimal("950.00"));

        CashFlowAggregate result = aggregationService.aggregate("acc-10293847", LocalDate.of(2026, 8, 9));

        assertThat(result.closingBalance()).isEqualByComparingTo("49213.55");
        assertThat(result.openingBalance()).isEqualByComparingTo("46963.55");
        assertThat(result.totalCredits()).isEqualByComparingTo("3200.00");
        assertThat(result.totalDebits()).isEqualByComparingTo("950.00");
    }

    @Test
    void aggregate_closingBalanceIsStable_regardlessOfLaterActivityOnTheLiveAccount() throws Exception {
        // The account's live current_balance has since moved on (e.g. later
        // days' activity), but the report for this fixed historical day
        // must reflect only entries posted before its own cutoff.
        AccountEntity account = new AccountEntity();
        setField(account, "accountId", "acc-10293847");
        setField(account, "openingBalance", new BigDecimal("0.00"));
        setField(account, "currentBalance", new BigDecimal("600.00"));
        when(accountRepository.findById("acc-10293847")).thenReturn(Optional.of(account));
        when(ledgerEntryRepository.sumCredits(any(), any(), any())).thenReturn(new BigDecimal("100.00"));
        when(ledgerEntryRepository.sumDebits(any(), any(), any())).thenReturn(BigDecimal.ZERO);
        when(ledgerEntryRepository.sumCreditsBefore(any(), any())).thenReturn(new BigDecimal("100.00"));
        when(ledgerEntryRepository.sumDebitsBefore(any(), any())).thenReturn(BigDecimal.ZERO);

        CashFlowAggregate result = aggregationService.aggregate("acc-10293847", LocalDate.of(2026, 8, 5));

        assertThat(result.closingBalance()).isEqualByComparingTo("100.00");
        assertThat(result.openingBalance()).isEqualByComparingTo("0.00");
    }

    @Test
    void aggregate_throwsAccountNotFound_whenAccountDoesNotExist() {
        when(accountRepository.findById("acc-missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> aggregationService.aggregate("acc-missing", LocalDate.now()))
                .isInstanceOf(AccountNotFoundException.class);
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
