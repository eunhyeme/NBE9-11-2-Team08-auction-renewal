package com.team8.backend.global.ratelimit;

import com.team8.backend.global.util.ClientIpResolver;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(RateLimitProperties.class)
public class RateLimitConfig {

    @Bean
    public RateLimiter rateLimiter(RateLimitProperties properties) {
        return new RateLimiter(properties);
    }

    @Bean
    public RateLimitFilter rateLimitFilter(RateLimitProperties properties,
                                           RateLimiter rateLimiter,
                                           ClientIpResolver clientIpResolver) {
        return new RateLimitFilter(properties, rateLimiter, clientIpResolver);
    }

    /**
     * Filter 빈은 서블릿 필터로도 자동 등록되므로 꺼 둔다.
     * 이 필터는 SecurityConfig 에서 Security 필터 체인 안에만 등록한다.
     */
    @Bean
    public FilterRegistrationBean<RateLimitFilter> rateLimitFilterRegistration(RateLimitFilter rateLimitFilter) {
        FilterRegistrationBean<RateLimitFilter> registration = new FilterRegistrationBean<>(rateLimitFilter);
        registration.setEnabled(false);
        return registration;
    }
}
