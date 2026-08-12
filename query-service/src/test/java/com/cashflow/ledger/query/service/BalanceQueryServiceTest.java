package com.cashflow.ledger.query.service;

import com.cashflow.ledger.query.cache.BalanceCacheService;
import com.cashflow.ledger.query.entity.AccountEntity;
import com.cashflow.ledger.query.repository.AccountRepository;
import com.cashflow.ledger.query.web.BalanceResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BalanceQueryServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private BalanceCacheService balanceCacheService;

    @InjectMocks
    private BalanceQueryService balanceQueryService;

    @Test
    void getBalance_returnsCachedValue_whenPresentInCache() {
        BalanceResponse cached = new BalanceResponse("acc-1", new BigDecimal("100.00"), "USD", Instant.now(), "db");
        when(balanceCacheService.get("acc-1")).thenReturn(Optional.of(cached));

        BalanceResponse result = balanceQueryService.getBalance("acc-1");

        assertThat(result.source()).isEqualTo("cache");
        assertThat(result.currentBalance()).isEqualByComparingTo("100.00");
        verify(accountRepository, never()).findById(any());
    }

    @Test
    void getBalance_loadsFromRepositoryAndCaches_whenCacheMiss() throws Exception {
        when(balanceCacheService.get("acc-1")).thenReturn(Optional.empty());
        AccountEntity account = buildAccount("acc-1", new BigDecimal("500.00"), "USD");
        when(accountRepository.findById("acc-1")).thenReturn(Optional.of(account));

        BalanceResponse result = balanceQueryService.getBalance("acc-1");

        assertThat(result.source()).isEqualTo("db");
        assertThat(result.currentBalance()).isEqualByComparingTo("500.00");
        verify(balanceCacheService, times(1)).put(eq("acc-1"), any(BalanceResponse.class));
    }

    @Test
    void getBalance_throwsAccountNotFound_whenAccountDoesNotExist() {
        when(balanceCacheService.get("acc-missing")).thenReturn(Optional.empty());
        when(accountRepository.findById("acc-missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> balanceQueryService.getBalance("acc-missing"))
                .isInstanceOf(AccountNotFoundException.class);
    }

    private AccountEntity buildAccount(String accountId, BigDecimal balance, String currency) throws Exception {
        AccountEntity account = new AccountEntity();
        setField(account, "accountId", accountId);
        setField(account, "accountName", "Test Account");
        setField(account, "currency", currency);
        setField(account, "openingBalance", balance);
        setField(account, "currentBalance", balance);
        setField(account, "updatedAt", Instant.now());
        return account;
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
