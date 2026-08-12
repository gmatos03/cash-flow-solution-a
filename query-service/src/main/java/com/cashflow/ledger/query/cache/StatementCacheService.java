package com.cashflow.ledger.query.cache;

import com.cashflow.ledger.query.web.StatementResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDate;
import java.util.Optional;

/**
 * Cache-aside layer for GET /queries/accounts/{id}/statement
 * (Appendix F.8: {@code statement:{accountId}:{from}:{to}:{page}:{size}}, 60s TTL).
 */
@Component
public class StatementCacheService {

    private static final Logger log = LoggerFactory.getLogger(StatementCacheService.class);
    private static final String KEY_PREFIX = "statement:";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final Duration ttl;

    public StatementCacheService(StringRedisTemplate redisTemplate,
                                  ObjectMapper objectMapper,
                                  @Value("${app.cache.statement-ttl-seconds}") long ttlSeconds) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.ttl = Duration.ofSeconds(ttlSeconds);
    }

    public String buildKey(String accountId, LocalDate from, LocalDate to, int page, int size) {
        return KEY_PREFIX + accountId + ":" + from + ":" + to + ":" + page + ":" + size;
    }

    public Optional<StatementResponse> get(String key) {
        try {
            String json = redisTemplate.opsForValue().get(key);
            if (json == null) {
                return Optional.empty();
            }
            return Optional.of(objectMapper.readValue(json, StatementResponse.class));
        } catch (Exception ex) {
            log.warn("Failed to read statement cache for {}: {}", key, ex.getMessage());
            return Optional.empty();
        }
    }

    public void put(String key, StatementResponse response) {
        try {
            String json = objectMapper.writeValueAsString(response);
            redisTemplate.opsForValue().set(key, json, ttl);
        } catch (Exception ex) {
            log.warn("Failed to write statement cache for {}: {}", key, ex.getMessage());
        }
    }
}
