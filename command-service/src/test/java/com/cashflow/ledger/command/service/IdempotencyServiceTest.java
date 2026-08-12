package com.cashflow.ledger.command.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IdempotencyServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private IdempotencyService idempotencyService;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        idempotencyService = new IdempotencyService(redisTemplate, 24);
    }

    @Test
    void reserve_returnsEmpty_whenKeyIsClaimedForTheFirstTime() {
        when(valueOperations.setIfAbsent(eq("idem:key-1"), eq("ent-123"), any(Duration.class)))
                .thenReturn(true);

        Optional<String> result = idempotencyService.reserve("key-1", "ent-123");

        assertThat(result).isEmpty();
    }

    @Test
    void reserve_returnsOriginalEntryId_whenKeyWasAlreadyClaimed() {
        when(valueOperations.setIfAbsent(eq("idem:key-1"), eq("ent-456"), any(Duration.class)))
                .thenReturn(false);
        when(valueOperations.get("idem:key-1")).thenReturn("ent-123");

        Optional<String> result = idempotencyService.reserve("key-1", "ent-456");

        assertThat(result).contains("ent-123");
    }
}
