package com.cashflow.ledger.eventhandler.cache;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Invalidates the Query Service's balance cache entry immediately after a
 * projection is applied (Appendix F.8: {@code balance:{accountId}}).
 * Both services must agree on the key naming convention since they are
 * independent Maven projects that only share the Redis instance.
 */
@Component
public class BalanceCacheEvictor {

    private static final String KEY_PREFIX = "balance:";

    private final StringRedisTemplate redisTemplate;

    public BalanceCacheEvictor(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void evict(String accountId) {
        redisTemplate.delete(KEY_PREFIX + accountId);
    }
}
