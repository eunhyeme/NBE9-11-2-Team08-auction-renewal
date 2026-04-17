package com.team8.backend.global.websocket;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
@Slf4j
@Component
public class WebSocketConnectionInterceptor implements HandshakeInterceptor {

    // IP별 동시 연결 수
    @Getter
    private final Map<String, AtomicInteger> connectionCount = new ConcurrentHashMap<>();

    @Value("${websocket.max-connections-per-ip}")
    private int maxConnections;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request,
                                   ServerHttpResponse response,
                                   WebSocketHandler wsHandler,
                                   Map<String, Object> attributes) {

        InetSocketAddress remoteAddress = request.getRemoteAddress();

        if (remoteAddress == null || remoteAddress.getAddress() == null) {
            log.warn("RemoteAddress is null");
            return false;
        }

        String ip = remoteAddress.getAddress().getHostAddress();

        attributes.put("ip", ip);

        AtomicInteger count = connectionCount.computeIfAbsent(ip, k -> new AtomicInteger(0));

        int current = count.get();

        log.info("[WS-Interceptor] IP: {}, current: {}", ip, current);

        // 🚨 제한 초과
        if (current >= maxConnections) {
            log.warn("[WS-Interceptor] connection limit exceeded: {}", ip);
            return false;
        }

        // 🔥 원자적으로 증가
        count.incrementAndGet();

        log.info("[WS-Interceptor] connection accepted: {} -> {}", ip, count.get());

        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request,
                               ServerHttpResponse response,
                               WebSocketHandler wsHandler,
                               Exception exception) {
    }
}