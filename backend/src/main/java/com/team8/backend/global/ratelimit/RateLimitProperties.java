package com.team8.backend.global.ratelimit;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;
import java.util.List;

/**
 * 처리율 제한 설정 (application.yaml 의 rate-limit.*).
 * policies 는 위에서부터 순서대로 검사하며, 처음 매칭되는 정책 하나만 적용된다.
 */
@ConfigurationProperties("rate-limit")
public record RateLimitProperties(
        @DefaultValue("true") boolean enabled,
        @DefaultValue("10m") Duration cacheExpireAfterAccess,
        @DefaultValue("100000") long cacheMaximumSize,
        List<Policy> policies
) {

    public RateLimitProperties {
        policies = policies == null ? List.of() : List.copyOf(policies);
    }

    /**
     * 토큰 버킷 정책.
     *
     * @param name         버킷 키 접두어로 쓰이는 정책 이름 (정책끼리 버킷을 공유하지 않도록 고유해야 함)
     * @param method       HTTP 메서드. 비우면 모든 메서드에 적용
     * @param pattern      Ant 스타일 경로 패턴 (예: /api/orders/*&#47;pay)
     * @param key          버킷을 나누는 기준. USER 인데 비로그인 요청이면 IP 로 대체
     * @param capacity     버킷 최대 토큰 수 = 순간적으로 허용하는 최대 요청 수
     * @param refillTokens refillPeriod 마다 채워지는 토큰 수
     * @param refillPeriod 충전 주기
     */
    public record Policy(
            String name,
            String method,
            String pattern,
            @DefaultValue("IP") KeyType key,
            long capacity,
            long refillTokens,
            Duration refillPeriod
    ) {
    }

    public enum KeyType {
        IP, USER
    }
}
