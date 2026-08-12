package com.cashflow.ledger.eventhandler.cache;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * Invalidates the Query Service's statement cache entries immediately after
 * a projection is applied (Appendix F.8: {@code statement:{accountId}:{from}:{to}:{page}:{size}}).
 * The key embeds the query's from/to/page/size, so unlike the balance cache
 * this can't be a single-key delete - every cached page/range for the
 * account has to be matched and cleared, or a stale statement page (e.g. an
 * empty result cached moments before the entry projected) would otherwise
 * outlive the projection for up to the cache's TTL.
 */
@Component
public class StatementCacheEvictor {

    private static final String KEY_PREFIX = "statement:";

    private final StringRedisTemplate redisTemplate;

    public StatementCacheEvictor(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void evict(String accountId) {
        Set<String> keys = redisTemplate.keys(KEY_PREFIX + accountId + ":*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }
}
