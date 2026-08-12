package com.cashflow.ledger.reporting.service;

import com.cashflow.ledger.reporting.domain.CashFlowAggregate;
import com.cashflow.ledger.reporting.entity.AccountEntity;
import com.cashflow.ledger.reporting.repository.AccountRepository;
import com.cashflow.ledger.reporting.repository.DailyCashFlowLogRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DailyCashFlowReportJobTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private DailyCashFlowLogRepository dailyCashFlowLogRepository;

    @Mock
    private CashFlowAggregationService aggregationService;

    @Mock
    private ReportRenderer reportRenderer;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private DailyCashFlowReportJob reportJob;

    @Test
    void runForAllAccounts_processesEveryAccountAndNotifies() throws Exception {
        LocalDate reportDate = LocalDate.of(2026, 8, 9);
        AccountEntity account1 = buildAccount("acc-1");
        AccountEntity account2 = buildAccount("acc-2");
        when(accountRepository.findAll()).thenReturn(List.of(account1, account2));

        CashFlowAggregate agg1 = new CashFlowAggregate("acc-1", reportDate, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
        CashFlowAggregate agg2 = new CashFlowAggregate("acc-2", reportDate, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
        when(aggregationService.aggregate("acc-1", reportDate)).thenReturn(agg1);
        when(aggregationService.aggregate("acc-2", reportDate)).thenReturn(agg2);
        when(reportRenderer.renderCsv(any())).thenReturn("csv");
        when(reportRenderer.renderPdf(any())).thenReturn(new byte[] {1, 2, 3});
        when(reportRenderer.writeToLocalStorage(any(), any(), any())).thenReturn(Path.of("/tmp/reports"));

        int processed = reportJob.runForAllAccounts(reportDate);

        assertThat(processed).isEqualTo(2);
        verify(dailyCashFlowLogRepository, times(2)).upsert(any(), any(), any(), any(), any(), any());
        verify(notificationService, times(2)).notifyReportReady(any(), any());
    }

    @Test
    void runForAllAccounts_continuesAfterOneAccountFails() {
        LocalDate reportDate = LocalDate.of(2026, 8, 9);
        AccountEntity account1 = buildAccountQuiet("acc-1");
        AccountEntity account2 = buildAccountQuiet("acc-2");
        when(accountRepository.findAll()).thenReturn(List.of(account1, account2));
        when(aggregationService.aggregate("acc-1", reportDate)).thenThrow(new RuntimeException("boom"));
        when(aggregationService.aggregate("acc-2", reportDate)).thenReturn(
                new CashFlowAggregate("acc-2", reportDate, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO));
        when(reportRenderer.renderCsv(any())).thenReturn("csv");
        when(reportRenderer.renderPdf(any())).thenReturn(new byte[] {1});
        when(reportRenderer.writeToLocalStorage(any(), any(), any())).thenReturn(Path.of("/tmp/reports"));

        int processed = reportJob.runForAllAccounts(reportDate);

        assertThat(processed).isEqualTo(1);
    }

    private AccountEntity buildAccount(String accountId) throws Exception {
        AccountEntity account = new AccountEntity();
        setField(account, "accountId", accountId);
        setField(account, "currentBalance", BigDecimal.ZERO);
        return account;
    }

    private AccountEntity buildAccountQuiet(String accountId) {
        try {
            return buildAccount(accountId);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
