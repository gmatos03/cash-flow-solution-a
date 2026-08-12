package com.cashflow.ledger.eventhandler.kafka;

import com.cashflow.ledger.eventhandler.domain.CreditDebitEvent;
import com.cashflow.ledger.eventhandler.service.EventProjectionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Consumer group "event-handler-service" on ledger.credit-debit.events
 * (Appendix F.5). Poison messages are routed to the .dlq topic by the
 * error handler configured in KafkaConsumerConfig after 3 retries.
 */
@Component
public class CreditDebitEventListener {

    private static final Logger log = LoggerFactory.getLogger(CreditDebitEventListener.class);

    private final EventProjectionService projectionService;

    public CreditDebitEventListener(EventProjectionService projectionService) {
        this.projectionService = projectionService;
    }

    @KafkaListener(topics = "${app.kafka.topic.credit-debit-events}")
    public void onCreditDebitEvent(CreditDebitEvent event) {
        log.debug("Received event {} for account {}", event.eventId(), event.accountId());
        projectionService.project(event);
    }
}
