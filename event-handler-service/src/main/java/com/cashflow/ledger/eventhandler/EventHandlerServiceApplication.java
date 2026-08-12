package com.cashflow.ledger.eventhandler;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Solution A - Event Handler Service.
 * Consumes ledger.credit-debit.events from Kafka, projects the read model
 * (accounts, ledger_entries) in PostgreSQL, and invalidates the Redis
 * balance cache. Exposes no public REST API - see Appendix F.4.
 */
@SpringBootApplication
public class EventHandlerServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(EventHandlerServiceApplication.class, args);
    }
}
