package com.cashflow.ledger.command.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * Atomic check-and-set idempotency guard backed by Redis
 * (Appendix F.8: {@code idem:{idempotencyKey}}, 24h TTL).
 *
 * A single SET ... NX ... EX call is what makes this safe under concurrent
 * requests: only the first caller to reserve a given key succeeds.
 */
@Service
public class IdempotencyService {

    private static final String KEY_PREFIX = "idem:";

    private final StringRedisTemplate redisTemplate;
    private final Duration ttl;

    public IdempotencyService(StringRedisTemplate redisTemplate,
                               @Value("${app.idempotency.ttl-hours}") long ttlHours) {
        this.redisTemplate = redisTemplate;
        this.ttl = Duration.ofHours(ttlHours);
    }

    /**
     * Attempts to reserve {@code idempotencyKey} for {@code entryId}.
     *
     * @return the entryId already associated with this key if it was used
     *         before, or {@code Optional.empty()} if this call is the one
     *         that claimed it.
     */
    public java.util.Optional<String> reserve(String idempotencyKey, String entryId) {
        ValueOperations<String, String> ops = redisTemplate.opsForValue();
        String key = KEY_PREFIX + idempotencyKey;
        Boolean reserved = ops.setIfAbsent(key, entryId, ttl);

        if (Boolean.TRUE.equals(reserved)) {
            return java.util.Optional.empty();
        }
        String existing = ops.get(key);
        return java.util.Optional.ofNullable(existing);
    }

    public void release(String idempotencyKey) {
        redisTemplate.delete(KEY_PREFIX + idempotencyKey);
    }
}
