package com.cashflow.ledger.eventhandler.kafka;

import com.cashflow.ledger.eventhandler.domain.CreditDebitEvent;
import com.cashflow.ledger.eventhandler.domain.EntryType;
import com.cashflow.ledger.eventhandler.service.EventProjectionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CreditDebitEventListenerTest {

    @Mock
    private EventProjectionService projectionService;

    @InjectMocks
    private CreditDebitEventListener listener;

    @Test
    void onCreditDebitEvent_delegatesToProjectionService() {
        CreditDebitEvent event = new CreditDebitEvent(
                UUID.randomUUID(), "ent-1", "acc-10293847", EntryType.CREDIT,
                new BigDecimal("1250.00"), "USD", "WEB", "test", Instant.now(), 1);

        listener.onCreditDebitEvent(event);

        verify(projectionService, times(1)).project(event);
    }
}
