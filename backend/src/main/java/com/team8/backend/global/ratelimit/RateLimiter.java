package com.team8.backend.global.ratelimit;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;

/**
 * 정책 + 키(사용자 / IP) 조합마다 토큰 버킷을 하나씩 메모리에 보관한다.
 * 일정 시간 접근이 없는 버킷은 Caffeine 이 자동으로 정리하므로 키가 무한정 쌓이지 않는다.
 * 서버를 여러 대로 늘리면 이 클래스만 bucket4j-redis 기반 ProxyManager 로 교체하면 된다.
 */
public class RateLimiter {

    private final Cache<String, Bucket> buckets;

    public RateLimiter(RateLimitProperties properties) {
        this.buckets = Caffeine.newBuilder()
                .expireAfterAccess(properties.cacheExpireAfterAccess())
                .maximumSize(properties.cacheMaximumSize())
                .build();
    }

    public ConsumptionProbe tryConsume(RateLimitProperties.Policy policy, String key) {
        Bucket bucket = buckets.get(policy.name() + ":" + key, k -> newBucket(policy));
        return bucket.tryConsumeAndReturnRemaining(1);
    }

    private Bucket newBucket(RateLimitProperties.Policy policy) {
        return Bucket.builder()
                .addLimit(Bandwidth.builder()
                        .capacity(policy.capacity())
                        .refillGreedy(policy.refillTokens(), policy.refillPeriod())
                        .build())
                .build();
    }
}
