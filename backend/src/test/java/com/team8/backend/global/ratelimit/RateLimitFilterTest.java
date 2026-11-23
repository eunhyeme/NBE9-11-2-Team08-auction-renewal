package com.team8.backend.global.ratelimit;

import com.team8.backend.domain.user.entity.UserRole;
import com.team8.backend.global.security.CustomUserDetails;
import com.team8.backend.global.util.ClientIpResolver;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RateLimitFilterTest {

    private static final RateLimitProperties.Policy LOGIN = new RateLimitProperties.Policy(
            "login", "POST", "/api/users/login", RateLimitProperties.KeyType.IP, 3, 3, Duration.ofMinutes(1));
    private static final RateLimitProperties.Policy BID = new RateLimitProperties.Policy(
            "bid", "POST", "/api/auctions/bids/**", RateLimitProperties.KeyType.USER, 2, 1, Duration.ofMinutes(1));
    private static final RateLimitProperties.Policy PAY = new RateLimitProperties.Policy(
            "order-pay", "POST", "/api/orders/*/pay", RateLimitProperties.KeyType.USER, 1, 1, Duration.ofMinutes(1));

    private RateLimitFilter filter = newFilter(true, false);

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private static RateLimitFilter newFilter(boolean enabled, boolean trustForwardedFor) {
        RateLimitProperties properties = new RateLimitProperties(
                enabled, Duration.ofMinutes(10), 1000, List.of(LOGIN, BID, PAY));
        return new RateLimitFilter(properties, new RateLimiter(properties), new ClientIpResolver(trustForwardedFor));
    }

    private MockHttpServletResponse call(String method, String uri, String ip) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest(method, uri);
        request.setRemoteAddr(ip);
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, new MockFilterChain());
        return response;
    }

    private void loginAs(long userId) {
        CustomUserDetails user = new CustomUserDetails(userId, "u" + userId + "@test.com", UserRole.values()[0], "nick");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));
    }

    @Nested
    @DisplayName("IP 기준 정책 (로그인)")
    class IpPolicy {

        @Test
        @DisplayName("capacity 까지는 통과, 초과 시 429 + Retry-After + 공통 에러 응답")
        void overCapacity_returns429() throws Exception {
            for (int i = 0; i < 3; i++) {
                MockHttpServletResponse ok = call("POST", "/api/users/login", "1.1.1.1");
                assertThat(ok.getStatus()).isEqualTo(200);
                assertThat(ok.getHeader(RateLimitFilter.HEADER_REMAINING)).isEqualTo(String.valueOf(2 - i));
            }

            MockHttpServletResponse rejected = call("POST", "/api/users/login", "1.1.1.1");

            assertThat(rejected.getStatus()).isEqualTo(429);
            assertThat(Long.parseLong(rejected.getHeader(RateLimitFilter.HEADER_RETRY_AFTER))).isPositive();
            assertThat(rejected.getContentAsString()).contains("429-1");
        }

        @Test
        @DisplayName("다른 IP 는 별도 버킷")
        void differentIp_separateBucket() throws Exception {
            for (int i = 0; i < 3; i++) {
                call("POST", "/api/users/login", "1.1.1.1");
            }

            assertThat(call("POST", "/api/users/login", "2.2.2.2").getStatus()).isEqualTo(200);
        }

        @Test
        @DisplayName("trust-forwarded-for 가 꺼져 있으면 X-Forwarded-For 위조로 우회 불가")
        void forwardedForIgnoredByDefault() throws Exception {
            for (int i = 0; i < 3; i++) {
                call("POST", "/api/users/login", "1.1.1.1");
            }
            MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/users/login");
            request.setRemoteAddr("1.1.1.1");
            request.addHeader("X-Forwarded-For", "9.9.9.9");
            MockHttpServletResponse response = new MockHttpServletResponse();

            filter.doFilter(request, response, new MockFilterChain());

            assertThat(response.getStatus()).isEqualTo(429);
        }

        @Test
        @DisplayName("trust-forwarded-for 가 켜져 있으면 X-Forwarded-For 첫 번째 IP 기준")
        void forwardedForTrusted() throws Exception {
            filter = newFilter(true, true);
            for (int i = 0; i < 3; i++) {
                MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/users/login");
                request.setRemoteAddr("10.0.0.1"); // 프록시
                request.addHeader("X-Forwarded-For", "7.7.7.7, 10.0.0.1");
                filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());
            }
            MockHttpServletRequest otherClient = new MockHttpServletRequest("POST", "/api/users/login");
            otherClient.setRemoteAddr("10.0.0.1");
            otherClient.addHeader("X-Forwarded-For", "8.8.8.8");
            MockHttpServletResponse response = new MockHttpServletResponse();

            filter.doFilter(otherClient, response, new MockFilterChain());

            assertThat(response.getStatus()).isEqualTo(200);
        }
    }

    @Nested
    @DisplayName("사용자 기준 정책 (입찰)")
    class UserPolicy {

        @Test
        @DisplayName("같은 IP 라도 사용자가 다르면 별도 버킷")
        void differentUsers_sameIp_separateBuckets() throws Exception {
            loginAs(1L);
            call("POST", "/api/auctions/bids", "3.3.3.3");
            call("POST", "/api/auctions/bids/atomic", "3.3.3.3");
            assertThat(call("POST", "/api/auctions/bids", "3.3.3.3").getStatus()).isEqualTo(429);

            loginAs(2L);
            assertThat(call("POST", "/api/auctions/bids", "3.3.3.3").getStatus()).isEqualTo(200);
        }

        @Test
        @DisplayName("같은 사용자는 IP 가 바뀌어도 같은 버킷")
        void sameUser_differentIp_sameBucket() throws Exception {
            loginAs(1L);
            call("POST", "/api/auctions/bids", "3.3.3.3");
            call("POST", "/api/auctions/bids", "4.4.4.4");

            assertThat(call("POST", "/api/auctions/bids", "5.5.5.5").getStatus()).isEqualTo(429);
        }

        @Test
        @DisplayName("경로 변수 패턴 매칭 (/api/orders/*/pay)")
        void pathVariablePattern() throws Exception {
            loginAs(1L);
            assertThat(call("POST", "/api/orders/10/pay", "3.3.3.3").getStatus()).isEqualTo(200);
            assertThat(call("POST", "/api/orders/11/pay", "3.3.3.3").getStatus()).isEqualTo(429);
        }
    }

    @Nested
    @DisplayName("제외 대상")
    class Excluded {

        @Test
        @DisplayName("매칭되는 정책이 없으면 제한하지 않고 헤더도 붙이지 않음")
        void noMatchingPolicy_passes() throws Exception {
            for (int i = 0; i < 10; i++) {
                MockHttpServletResponse response = call("POST", "/api/users/logout", "1.1.1.1");
                assertThat(response.getStatus()).isEqualTo(200);
                assertThat(response.getHeader(RateLimitFilter.HEADER_LIMIT)).isNull();
            }
        }

        @Test
        @DisplayName("메서드가 다르면 매칭되지 않음 (GET /api/users/login)")
        void differentMethod_passes() throws Exception {
            for (int i = 0; i < 10; i++) {
                assertThat(call("GET", "/api/users/login", "1.1.1.1").getStatus()).isEqualTo(200);
            }
        }

        @Test
        @DisplayName("CORS preflight(OPTIONS) 는 제한하지 않음")
        void preflight_passes() throws Exception {
            for (int i = 0; i < 10; i++) {
                assertThat(call("OPTIONS", "/api/users/login", "1.1.1.1").getStatus()).isEqualTo(200);
            }
        }

        @Test
        @DisplayName("enabled=false 면 전부 통과")
        void disabled_passes() throws Exception {
            filter = newFilter(false, false);
            for (int i = 0; i < 10; i++) {
                assertThat(call("POST", "/api/users/login", "1.1.1.1").getStatus()).isEqualTo(200);
            }
        }
    }
}
