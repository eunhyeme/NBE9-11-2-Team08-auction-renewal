package com.team8.backend.global.util;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.net.InetSocketAddress;

/**
 * 클라이언트 IP 추출.
 * trust-forwarded-for 가 true 일 때만 X-Forwarded-For 의 첫 번째 값을 신뢰한다.
 * 프록시 없이 직접 노출된 서버에서 이 헤더를 믿으면 클라이언트가 IP 를 위조해 제한을 우회할 수 있으므로
 * Nginx/로드밸런서 뒤에 배포할 때만 켠다.
 */
@Component
public class ClientIpResolver {

    private static final String X_FORWARDED_FOR = "X-Forwarded-For";

    private final boolean trustForwardedFor;

    public ClientIpResolver(@Value("${client-ip.trust-forwarded-for:false}") boolean trustForwardedFor) {
        this.trustForwardedFor = trustForwardedFor;
    }

    public String resolve(HttpServletRequest request) {
        if (trustForwardedFor) {
            String forwarded = firstForwarded(request.getHeader(X_FORWARDED_FOR));
            if (forwarded != null) {
                return forwarded;
            }
        }
        return request.getRemoteAddr();
    }

    /** WebSocket 핸드셰이크용. IP 를 알 수 없으면 null. */
    public String resolve(ServerHttpRequest request) {
        if (trustForwardedFor) {
            String forwarded = firstForwarded(request.getHeaders().getFirst(X_FORWARDED_FOR));
            if (forwarded != null) {
                return forwarded;
            }
        }
        InetSocketAddress remoteAddress = request.getRemoteAddress();
        if (remoteAddress == null || remoteAddress.getAddress() == null) {
            return null;
        }
        return remoteAddress.getAddress().getHostAddress();
    }

    private String firstForwarded(String header) {
        if (!StringUtils.hasText(header)) {
            return null;
        }
        String first = header.split(",")[0].trim();
        return first.isEmpty() ? null : first;
    }
}
