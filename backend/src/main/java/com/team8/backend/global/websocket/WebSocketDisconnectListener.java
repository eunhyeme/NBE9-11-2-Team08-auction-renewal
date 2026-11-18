package com.team8.backend.global.websocket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.util.Map;


@Slf4j
@Component
public class WebSocketDisconnectListener {

    private final WebSocketConnectionInterceptor interceptor;

    public WebSocketDisconnectListener(WebSocketConnectionInterceptor interceptor) {
        this.interceptor = interceptor;
    }

    @EventListener
    public void handleDisconnect(SessionDisconnectEvent event) {

        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());

        Map<String, Object> sessionAttributes = accessor.getSessionAttributes();

        if (sessionAttributes == null) {
            return;
        }

        // SessionDisconnectEvent 는 한 세션에 대해 여러 번 발행될 수 있다 (Spring 문서 명시).
        // 세션 속성에서 ip 를 꺼내면서 지워, 같은 세션의 중복 이벤트가 카운터를 두 번 줄이지 않게 한다.
        String ip = (String) sessionAttributes.remove(WebSocketConnectionInterceptor.IP_ATTRIBUTE);

        if (ip == null) {
            return;
        }

        int remaining = interceptor.release(ip);

        if (remaining < 0) {
            log.warn("[WS-Disconnect] no counter found for ip: {}", ip);
            return;
        }

        log.info("[WS-Disconnect] disconnected ip: {}, remaining: {}", ip, remaining);
    }
}
