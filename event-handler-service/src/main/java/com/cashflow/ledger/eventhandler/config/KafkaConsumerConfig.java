package com.cashflow.ledger.eventhandler.config;

import org.apache.kafka.common.TopicPartition;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.boot.autoconfigure.kafka.ConcurrentKafkaListenerContainerFactoryConfigurer;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

/**
 * Wires a bounded-retry error handler that republishes a record to
 * ledger.credit-debit.events.dlq after repeated processing failures
 * (Appendix F.5).
 */
@Configuration
@EnableKafka
public class KafkaConsumerConfig {

    @Bean
    public DefaultErrorHandler kafkaErrorHandler(
            KafkaTemplate<Object, Object> kafkaTemplate,
            @Value("${app.kafka.listener.retry-interval-ms}") long retryIntervalMs,
            @Value("${app.kafka.listener.max-retry-attempts}") long maxRetryAttempts) {

        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(kafkaTemplate,
                (record, ex) -> new TopicPartition(record.topic() + ".dlq", record.partition()));

        return new DefaultErrorHandler(recoverer, new FixedBackOff(retryIntervalMs, maxRetryAttempts));
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<Object, Object> kafkaListenerContainerFactory(
            ConcurrentKafkaListenerContainerFactoryConfigurer configurer,
            ConsumerFactory<Object, Object> kafkaConsumerFactory,
            DefaultErrorHandler kafkaErrorHandler,
            @Value("${app.kafka.listener.concurrency}") int concurrency) {

        ConcurrentKafkaListenerContainerFactory<Object, Object> factory = new ConcurrentKafkaListenerContainerFactory<>();
        configurer.configure(factory, kafkaConsumerFactory);
        factory.setConcurrency(concurrency);
        factory.setCommonErrorHandler(kafkaErrorHandler);
        return factory;
    }
}
