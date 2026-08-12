package com.cashflow.ledger.query;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Solution A - Query Service.
 * Serves balances, statements, and the daily cash-flow log from the
 * PostgreSQL read model, through a Redis cache-aside layer. See Appendix D
 * and Appendix F of the Cash-Flow Architecture Report.
 */
@SpringBootApplication
public class QueryServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(QueryServiceApplication.class, args);
    }
}
