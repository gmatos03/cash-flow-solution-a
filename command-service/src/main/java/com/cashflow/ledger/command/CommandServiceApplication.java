package com.cashflow.ledger.command;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Solution A - Command Service.
 * Validates ledger entries, appends events to the Aurora/PostgreSQL event
 * store, and publishes them to Kafka for the Event Handler Service to
 * project. See Appendix D and Appendix F of the Cash-Flow Architecture
 * Report for the full design.
 */
@SpringBootApplication
public class CommandServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(CommandServiceApplication.class, args);
    }
}
