package com.team8.backend.webSocket;

import com.team8.backend.global.websocket.WebSocketConnectionInterceptor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.socket.WebSocketHandler;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

class WebSocketConnectionInterceptorTest {

    private WebSocketConnectionInterceptor interceptor;
    private ServerHttpRequest  request;
    private ServerHttpResponse response;
    private WebSocketHandler   wsHandler;
    private Map<String, Object> attributes;

    @BeforeEach
    void setUp() {
        interceptor = new WebSocketConnectionInterceptor();
        // @Value 필드 직접 주입 (스프링 컨텍스트 없이)
        ReflectionTestUtils.setField(interceptor, "maxConnections", 3);

        request    = mock(ServerHttpRequest.class);
        response   = mock(ServerHttpResponse.class);
        wsHandler  = mock(WebSocketHandler.class);
        attributes = new HashMap<>();
    }

    // ═══════════════════════════════════════════════
    // beforeHandshake
    // ═══════════════════════════════════════════════
    @Nested
    @DisplayName("beforeHandshake")
    class BeforeHandshake {

        @Test
        @DisplayName("첫 연결 → true 반환, 카운터 1 증가")
        void firstConnection_allowed() throws Exception {
            givenRemoteAddress("192.168.1.1");

            boolean result = interceptor.beforeHandshake(request, response, wsHandler, attributes);

            assertThat(result).isTrue();
            assertThat(attributes.get("ip")).isEqualTo("192.168.1.1");
            assertThat(interceptor.getConnectionCount().get("192.168.1.1").get()).isEqualTo(1);
        }

        @Test
        @DisplayName("maxConnections 미만 연결 → 모두 허용")
        void belowLimit_allAllowed() throws Exception {
            givenRemoteAddress("10.0.0.1");

            for (int i = 0; i < 3; i++) {
                boolean result = interceptor.beforeHandshake(request, response, wsHandler, new HashMap<>());
                assertThat(result).isTrue();
            }
            assertThat(interceptor.getConnectionCount().get("10.0.0.1").get()).isEqualTo(3);
        }

        @Test
        @DisplayName("maxConnections 초과 시도 → false 반환, 카운터 증가 없음")
        void overLimit_rejected() throws Exception {
            givenRemoteAddress("10.0.0.2");
            // maxConnections = 3 → 3번 채우기
            for (int i = 0; i < 3; i++) {
                interceptor.beforeHandshake(request, response, wsHandler, new HashMap<>());
            }

            // 4번째 시도
            boolean result = interceptor.beforeHandshake(request, response, wsHandler, attributes);

            assertThat(result).isFalse();
            assertThat(interceptor.getConnectionCount().get("10.0.0.2").get()).isEqualTo(3);
        }

        @Test
        @DisplayName("remoteAddress가 null → false 반환")
        void nullRemoteAddress_rejected() {
            given(request.getRemoteAddress()).willReturn(null);

            boolean result = interceptor.beforeHandshake(request, response, wsHandler, attributes);

            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("서로 다른 IP는 독립적으로 카운팅")
        void differentIps_countedIndependently() throws Exception {
            givenRemoteAddress("1.1.1.1");
            interceptor.beforeHandshake(request, response, wsHandler, new HashMap<>());
            interceptor.beforeHandshake(request, response, wsHandler, new HashMap<>());

            givenRemoteAddress("2.2.2.2");
            interceptor.beforeHandshake(request, response, wsHandler, new HashMap<>());

            assertThat(interceptor.getConnectionCount().get("1.1.1.1").get()).isEqualTo(2);
            assertThat(interceptor.getConnectionCount().get("2.2.2.2").get()).isEqualTo(1);
        }
    }

    // ═══════════════════════════════════════════════
    // 헬퍼
    // ═══════════════════════════════════════════════
    private void givenRemoteAddress(String ip) throws Exception {
        InetSocketAddress address = new InetSocketAddress(InetAddress.getByName(ip), 8080);
        given(request.getRemoteAddress()).willReturn(address);
    }
}
