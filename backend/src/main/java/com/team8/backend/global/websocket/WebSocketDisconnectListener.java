package com.team8.backend.global.websocket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;


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

        String ip = (String) sessionAttributes.get("ip");

        if (ip == null) {
            return;
        }

        Map<String, AtomicInteger> connectionCount = interceptor.getConnectionCount();

        AtomicInteger count = connectionCount.get(ip);

        if (count == null) {
            log.warn("[WS-Disconnect] no counter found for ip: {}", ip);
            return;
        }

        int updated = count.decrementAndGet();

        log.info("[WS-Disconnect] disconnected ip: {}, remaining: {}", ip, updated);

        // 🔥 0이면 제거 (메모리 정리)
        if (updated <= 0) {
            connectionCount.remove(ip);
        }
    }
}