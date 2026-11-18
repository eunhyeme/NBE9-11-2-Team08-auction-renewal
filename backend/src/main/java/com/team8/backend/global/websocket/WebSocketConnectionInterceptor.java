package com.team8.backend.global.websocket;

import com.team8.backend.global.util.ClientIpResolver;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * IP별 WebSocket 동시 연결 수 제한.
 * 카운터의 확인·증가·감소·삭제는 모두 ConcurrentHashMap.compute 계열 안에서 처리해
 * 같은 IP의 동시 핸드셰이크/해제가 겹쳐도 한도를 넘거나 카운트를 잃지 않는다.
 */
@Slf4j
@Component
public class WebSocketConnectionInterceptor implements HandshakeInterceptor {

    public static final String IP_ATTRIBUTE = "ip";

    // IP별 동시 연결 수
    @Getter
    private final Map<String, AtomicInteger> connectionCount = new ConcurrentHashMap<>();

    private final ClientIpResolver clientIpResolver;

    @Value("${websocket.max-connections-per-ip}")
    private int maxConnections;

    public WebSocketConnectionInterceptor(ClientIpResolver clientIpResolver) {
        this.clientIpResolver = clientIpResolver;
    }

    @Override
    public boolean beforeHandshake(ServerHttpRequest request,
                                   ServerHttpResponse response,
                                   WebSocketHandler wsHandler,
                                   Map<String, Object> attributes) {

        String ip = clientIpResolver.resolve(request);

        if (ip == null) {
            log.warn("RemoteAddress is null");
            return false;
        }

        if (!tryAcquire(ip)) {
            log.warn("[WS-Interceptor] connection limit exceeded: {}", ip);
            return false;
        }

        attributes.put(IP_ATTRIBUTE, ip);
        log.info("[WS-Interceptor] connection accepted: {}", ip);
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request,
                               ServerHttpResponse response,
                               WebSocketHandler wsHandler,
                               Exception exception) {
        // 핸드셰이크가 실패하면 세션이 만들어지지 않아 disconnect 이벤트도 오지 않으므로 여기서 반납
        if (exception != null) {
            String ip = clientIpResolver.resolve(request);
            if (ip != null) {
                release(ip);
            }
        }
    }

    /** 한도 미만이면 1 증가시키고 true. 확인과 증가가 하나의 원자적 연산으로 처리된다. */
    boolean tryAcquire(String ip) {
        boolean[] acquired = {false};
        connectionCount.compute(ip, (key, count) -> {
            AtomicInteger current = count == null ? new AtomicInteger() : count;
            if (current.get() < maxConnections) {
                current.incrementAndGet();
                acquired[0] = true;
            }
            return current.get() == 0 ? null : current;
        });
        return acquired[0];
    }

    /**
     * 1 감소시키고 0이 되면 맵에서 제거한다. 감소와 제거가 원자적으로 처리된다.
     *
     * @return 남은 연결 수, 카운터가 없었으면 -1
     */
    public int release(String ip) {
        int[] remaining = {-1};
        connectionCount.computeIfPresent(ip, (key, count) -> {
            remaining[0] = Math.max(0, count.decrementAndGet());
            return remaining[0] == 0 ? null : count;
        });
        return remaining[0];
    }
}
