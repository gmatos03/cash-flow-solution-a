package com.cashflow.ledger.query.cache;

import com.cashflow.ledger.query.web.BalanceResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;

/**
 * Cache-aside layer for GET /queries/accounts/{id}/balance
 * (Appendix F.8: {@code balance:{accountId}}, 300s TTL).
 */
@Component
public class BalanceCacheService {

    private static final Logger log = LoggerFactory.getLogger(BalanceCacheService.class);
    private static final String KEY_PREFIX = "balance:";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final Duration ttl;

    public BalanceCacheService(StringRedisTemplate redisTemplate,
                                ObjectMapper objectMapper,
                                @Value("${app.cache.balance-ttl-seconds}") long ttlSeconds) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.ttl = Duration.ofSeconds(ttlSeconds);
    }

    public Optional<BalanceResponse> get(String accountId) {
        try {
            String json = redisTemplate.opsForValue().get(KEY_PREFIX + accountId);
            if (json == null) {
                return Optional.empty();
            }
            return Optional.of(objectMapper.readValue(json, BalanceResponse.class));
        } catch (Exception ex) {
            log.warn("Failed to read balance cache for {}: {}", accountId, ex.getMessage());
            return Optional.empty();
        }
    }

    public void put(String accountId, BalanceResponse response) {
        try {
            String json = objectMapper.writeValueAsString(response);
            redisTemplate.opsForValue().set(KEY_PREFIX + accountId, json, ttl);
        } catch (Exception ex) {
            log.warn("Failed to write balance cache for {}: {}", accountId, ex.getMessage());
        }
    }

    public void evict(String accountId) {
        redisTemplate.delete(KEY_PREFIX + accountId);
    }
}
