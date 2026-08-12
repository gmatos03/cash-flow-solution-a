package com.cashflow.ledger.command.service;

import com.cashflow.ledger.command.domain.CreditDebitEvent;
import com.cashflow.ledger.command.domain.EntryType;
import com.cashflow.ledger.command.domain.PostEntryCommand;
import com.cashflow.ledger.command.kafka.EventPublisher;
import com.cashflow.ledger.command.repository.AccountEntity;
import com.cashflow.ledger.command.repository.AccountJpaRepository;
import com.cashflow.ledger.command.repository.EventStoreRepository;
import com.cashflow.ledger.command.web.EntryAcceptedResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PostEntryCommandHandlerTest {

    @Mock
    private EventStoreRepository eventStoreRepository;

    @Mock
    private EventPublisher eventPublisher;

    @Mock
    private IdempotencyService idempotencyService;

    @Mock
    private AccountJpaRepository accountRepository;

    @InjectMocks
    private PostEntryCommandHandler handler;

    private PostEntryCommand validCommand;

    @BeforeEach
    void setUp() {
        validCommand = new PostEntryCommand(
                null,
                "acc-10293847",
                new BigDecimal("1250.00"),
                "USD",
                EntryType.CREDIT,
                "WEB",
                "Wire transfer received",
                "5f2c1a3e-9b7d-4e21-8c4a-2d6f9b1e7a44"
        );
    }

    private AccountEntity buildAccount(String accountId, String currency) throws Exception {
        AccountEntity account = new AccountEntity();
        setField(account, "accountId", accountId);
        setField(account, "currency", currency);
        return account;
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    @Test
    void handle_appendsEventAndPublishesToKafka_whenRequestIsValid() throws Exception {
        when(accountRepository.findById("acc-10293847"))
                .thenReturn(Optional.of(buildAccount("acc-10293847", "USD")));
        when(idempotencyService.reserve(eq(validCommand.idempotencyKey()), anyString()))
                .thenReturn(Optional.empty());

        EntryAcceptedResponse response = handler.handle(validCommand);

        assertThat(response.status()).isEqualTo("ACCEPTED");
        assertThat(response.accountId()).isEqualTo("acc-10293847");
        assertThat(response.entryId()).startsWith("ent-");

        ArgumentCaptor<CreditDebitEvent> eventCaptor = ArgumentCaptor.forClass(CreditDebitEvent.class);
        verify(eventStoreRepository, times(1)).append(eventCaptor.capture());
        verify(eventPublisher, times(1)).publish(eventCaptor.getValue());

        CreditDebitEvent published = eventCaptor.getValue();
        assertThat(published.accountId()).isEqualTo("acc-10293847");
        assertThat(published.amount()).isEqualByComparingTo("1250.00");
        assertThat(published.type()).isEqualTo(EntryType.CREDIT);
    }

    @Test
    void handle_throwsIdempotencyConflict_whenKeyWasAlreadyUsed() throws Exception {
        when(accountRepository.findById("acc-10293847"))
                .thenReturn(Optional.of(buildAccount("acc-10293847", "USD")));
        when(idempotencyService.reserve(eq(validCommand.idempotencyKey()), anyString()))
                .thenReturn(Optional.of("ent-original1"));

        assertThatThrownBy(() -> handler.handle(validCommand))
                .isInstanceOf(IdempotencyConflictException.class)
                .extracting(ex -> ((IdempotencyConflictException) ex).getExistingEntryId())
                .isEqualTo("ent-original1");

        verify(eventStoreRepository, never()).append(any());
        verify(eventPublisher, never()).publish(any());
    }

    @Test
    void handle_throwsInvalidAccount_whenAccountIdDoesNotMatchExpectedFormat() {
        PostEntryCommand malformed = new PostEntryCommand(
                null, "not-a-valid-id", new BigDecimal("10.00"), "USD",
                EntryType.DEBIT, "WEB", null, "some-idem-key");

        assertThatThrownBy(() -> handler.handle(malformed))
                .isInstanceOf(InvalidAccountException.class);

        verify(idempotencyService, never()).reserve(anyString(), anyString());
        verify(eventStoreRepository, never()).append(any());
    }

    @Test
    void handle_throwsInvalidAccount_whenAccountDoesNotExist() {
        when(accountRepository.findById("acc-10293847")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> handler.handle(validCommand))
                .isInstanceOf(InvalidAccountException.class);

        verify(idempotencyService, never()).reserve(anyString(), anyString());
        verify(eventStoreRepository, never()).append(any());
    }

    @Test
    void handle_throwsCurrencyMismatch_whenEntryCurrencyDiffersFromAccountCurrency() throws Exception {
        when(accountRepository.findById("acc-10293847"))
                .thenReturn(Optional.of(buildAccount("acc-10293847", "USD")));
        PostEntryCommand eurEntry = new PostEntryCommand(
                null, "acc-10293847", new BigDecimal("300.00"), "EUR",
                EntryType.CREDIT, "WEB", "wrong currency", "some-idem-key");

        assertThatThrownBy(() -> handler.handle(eurEntry))
                .isInstanceOf(CurrencyMismatchException.class);

        verify(idempotencyService, never()).reserve(anyString(), anyString());
        verify(eventStoreRepository, never()).append(any());
        verify(eventPublisher, never()).publish(any());
    }
}
