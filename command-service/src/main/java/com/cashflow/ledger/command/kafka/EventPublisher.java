package com.cashflow.ledger.command.kafka;

import com.cashflow.ledger.command.domain.CreditDebitEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Publishes domain events onto the ledger.credit-debit.events topic
 * (Appendix F.5), keyed by accountId so every event for a given account is
 * consumed in order by the Event Handler Service.
 */
@Component
public class EventPublisher {

    private static final Logger log = LoggerFactory.getLogger(EventPublisher.class);

    private final KafkaTemplate<String, CreditDebitEvent> kafkaTemplate;
    private final String topic;

    public EventPublisher(KafkaTemplate<String, CreditDebitEvent> kafkaTemplate,
                           @Value("${app.kafka.topic.credit-debit-events}") String topic) {
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
    }

    public void publish(CreditDebitEvent event) {
        kafkaTemplate.send(topic, event.accountId(), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish event {} for account {}: {}",
                                event.eventId(), event.accountId(), ex.getMessage(), ex);
                    } else {
                        log.debug("Published event {} to {}-{}@{}", event.eventId(), topic,
                                result.getRecordMetadata().partition(), result.getRecordMetadata().offset());
                    }
                });
    }
}
