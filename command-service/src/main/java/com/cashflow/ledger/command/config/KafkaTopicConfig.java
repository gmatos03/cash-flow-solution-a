package com.cashflow.ledger.command.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    @Bean
    public NewTopic creditDebitEventsTopic(
            @Value("${app.kafka.topic.credit-debit-events}") String topic,
            @Value("${app.kafka.topic.partitions}") int partitions,
            @Value("${app.kafka.topic.replicas}") short replicas) {
        return TopicBuilder.name(topic)
                .partitions(partitions)
                .replicas(replicas)
                .build();
    }
}
