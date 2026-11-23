package com.team8.backend.webSocket;

import com.team8.backend.global.util.ClientIpResolver;
import com.team8.backend.global.websocket.WebSocketConnectionInterceptor;
import com.team8.backend.global.websocket.WebSocketDisconnectListener;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class WebSocketDisconnectListenerTest {

    private WebSocketConnectionInterceptor interceptor;
    private WebSocketDisconnectListener disconnectListener;

    @BeforeEach
    void setUp() {
        interceptor = new WebSocketConnectionInterceptor(new ClientIpResolver(false));
        ReflectionTestUtils.setField(interceptor, "maxConnections", 3);
        disconnectListener = new WebSocketDisconnectListener(interceptor);
    }

    @Test
    @DisplayName("연결 해제 → 해당 IP 카운터 1 감소")
    void handleDisconnect_decreasesCounter() {
        String ip = "192.168.1.1";
        interceptor.getConnectionCount().put(ip, new AtomicInteger(2));

        SessionDisconnectEvent event = buildDisconnectEvent(ip);
        disconnectListener.handleDisconnect(event);

        assertThat(interceptor.getConnectionCount().get(ip).get()).isEqualTo(1);
    }

    @Test
    @DisplayName("카운터가 0이 되면 IP 맵에서 제거 (메모리 정리)")
    void handleDisconnect_removesEntryWhenZero() {
        String ip = "10.0.0.5";
        interceptor.getConnectionCount().put(ip, new AtomicInteger(1));

        SessionDisconnectEvent event = buildDisconnectEvent(ip);
        disconnectListener.handleDisconnect(event);

        assertThat(interceptor.getConnectionCount()).doesNotContainKey(ip);
    }

    @Test
    @DisplayName("같은 세션의 중복 disconnect 이벤트 → 카운터는 한 번만 감소")
    void handleDisconnect_duplicateEvent_decreasesOnce() {
        String ip = "172.16.0.1";
        interceptor.getConnectionCount().put(ip, new AtomicInteger(3));

        SessionDisconnectEvent event = buildDisconnectEvent(ip);
        disconnectListener.handleDisconnect(event);
        disconnectListener.handleDisconnect(event);

        assertThat(interceptor.getConnectionCount().get(ip).get()).isEqualTo(2);
    }

    @Test
    @DisplayName("sessionAttributes에 ip 없으면 아무것도 하지 않음")
    void handleDisconnect_noIpAttribute_doesNothing() {
        // ip 없이 빈 attributes
        SessionDisconnectEvent event = buildDisconnectEvent(null);

        // 예외 없이 정상 종료
        org.junit.jupiter.api.Assertions.assertDoesNotThrow(
                () -> disconnectListener.handleDisconnect(event)
        );
    }

    @Test
    @DisplayName("카운터가 없는 IP 연결 해제 → NPE 없이 warn 로그만 출력")
    void handleDisconnect_noCounter_doesNotThrow() {
        String ip = "99.99.99.99";
        // 카운터 없이 disconnect
        SessionDisconnectEvent event = buildDisconnectEvent(ip);

        org.junit.jupiter.api.Assertions.assertDoesNotThrow(
                () -> disconnectListener.handleDisconnect(event)
        );
    }

    // ═══════════════════════════════════════════════
    // 헬퍼
    // ═══════════════════════════════════════════════

    /**
     * STOMP DISCONNECT 메시지를 감싼 SessionDisconnectEvent 생성.
     * ip가 null이면 sessionAttributes에 ip 키를 넣지 않음.
     */
    private SessionDisconnectEvent buildDisconnectEvent(String ip) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.DISCONNECT);
        Map<String, Object> sessionAttributes = new HashMap<>();
        if (ip != null) {
            sessionAttributes.put("ip", ip);
        }
        accessor.setSessionAttributes(sessionAttributes);
        accessor.setSessionId("test-session-id");

        Message<byte[]> message = MessageBuilder
                .withPayload(new byte[0])
                .copyHeaders(accessor.toMap())
                .build();

        return new SessionDisconnectEvent(this, message, "test-session-id", null);
    }
}
