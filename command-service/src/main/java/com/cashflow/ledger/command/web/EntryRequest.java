package com.cashflow.ledger.command.web;

import com.cashflow.ledger.command.domain.EntryType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/** Request body for {@code POST /commands/entries} (Appendix F.2). */
public record EntryRequest(
        @NotBlank(message = "accountId is required") String accountId,

        @NotNull(message = "amount is required")
        @DecimalMin(value = "0.01", message = "amount must be greater than zero") BigDecimal amount,

        @NotBlank(message = "currency is required")
        @Size(min = 3, max = 3, message = "currency must be a 3-letter ISO code") String currency,

        @NotNull(message = "type is required") EntryType type,

        @NotBlank(message = "channel is required") String channel,

        @Size(max = 140, message = "description must be at most 140 characters") String description,

        @NotBlank(message = "idempotencyKey is required") String idempotencyKey
) {
}
