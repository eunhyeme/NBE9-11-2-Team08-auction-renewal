package com.team8.backend.global.ratelimit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.team8.backend.global.exception.ErrorCode;
import com.team8.backend.global.response.ApiResponse;
import com.team8.backend.global.security.CustomUserDetails;
import com.team8.backend.global.util.ClientIpResolver;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * API 처리율 제한 필터.
 * Security 필터 체인에서 JwtAuthenticationFilter 바로 뒤에 위치하므로
 * 로그인한 요청은 userId, 비로그인 요청은 IP 를 키로 사용할 수 있다.
 */
@Slf4j
public class RateLimitFilter extends OncePerRequestFilter {

    static final String HEADER_LIMIT = "X-RateLimit-Limit";
    static final String HEADER_REMAINING = "X-RateLimit-Remaining";
    static final String HEADER_RETRY_AFTER = "Retry-After";

    private final RateLimitProperties properties;
    private final RateLimiter rateLimiter;
    private final ClientIpResolver clientIpResolver;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public RateLimitFilter(RateLimitProperties properties, RateLimiter rateLimiter, ClientIpResolver clientIpResolver) {
        this.properties = properties;
        this.rateLimiter = rateLimiter;
        this.clientIpResolver = clientIpResolver;
    }

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        return !properties.enabled() || HttpMethod.OPTIONS.matches(request.getMethod());
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        RateLimitProperties.Policy policy = findPolicy(request);
        if (policy == null) {
            filterChain.doFilter(request, response);
            return;
        }

        String key = resolveKey(policy, request);
        ConsumptionProbe probe = rateLimiter.tryConsume(policy, key);

        response.setHeader(HEADER_LIMIT, String.valueOf(policy.capacity()));
        response.setHeader(HEADER_REMAINING, String.valueOf(probe.getRemainingTokens()));

        if (probe.isConsumed()) {
            filterChain.doFilter(request, response);
            return;
        }

        long retryAfterSeconds = Math.max(1, TimeUnit.NANOSECONDS.toSeconds(probe.getNanosToWaitForRefill() + 999_999_999L));
        log.warn("[RateLimit] rejected policy={}, key={}, {} {}, retryAfter={}s",
                policy.name(), key, request.getMethod(), request.getRequestURI(), retryAfterSeconds);

        ErrorCode errorCode = ErrorCode.TOO_MANY_REQUESTS;
        response.setStatus(errorCode.getHttpStatus().value());
        response.setHeader(HEADER_RETRY_AFTER, String.valueOf(retryAfterSeconds));
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(ApiResponse.error(errorCode)));
    }

    private RateLimitProperties.Policy findPolicy(HttpServletRequest request) {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        String method = request.getMethod();

        for (RateLimitProperties.Policy policy : properties.policies()) {
            boolean methodMatches = policy.method() == null || policy.method().equalsIgnoreCase(method);
            if (methodMatches && pathMatcher.match(policy.pattern(), path)) {
                return policy;
            }
        }
        return null;
    }

    private String resolveKey(RateLimitProperties.Policy policy, HttpServletRequest request) {
        if (policy.key() == RateLimitProperties.KeyType.USER) {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.getPrincipal() instanceof CustomUserDetails user) {
                return "user:" + user.getUserId();
            }
        }
        return "ip:" + clientIpResolver.resolve(request);
    }
}
