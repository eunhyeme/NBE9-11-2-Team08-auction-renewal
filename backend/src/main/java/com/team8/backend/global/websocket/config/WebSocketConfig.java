package com.team8.backend.global.websocket.config;

import com.team8.backend.global.websocket.WebSocketConnectionInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer { // 웹소켓 활성화 + STOMP 메시지브로커 디폴트사용


    private final WebSocketConnectionInterceptor connectionInterceptor;

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // 웹소켓 연결을 위한엔드포인트 등록
        registry.addEndpoint("/ws")                 //ws://localhost:8080/ws
                .addInterceptors(connectionInterceptor)   //같은 ip의 접근 수 검사하여 커넥션풀 고갈문제 해결
                .setAllowedOriginPatterns("*");     //일단 test동안 CORS 허용
//                .withSockJS();                    //postman은 sockJS가 아닌 순수 웹소켓 테스트만 지원해서 백엔드 개발동안은 꺼둠
    }

    // 브로드캐스팅에 필요한 메세지 브로커

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic"); // 클라이언트가 서버(메시지브로커)에 구독할 주소 + 예) /topic/auction/1
//        config.setApplicationDestinationPrefixes("/app"); // 발행
            // 클라이언트-> 서버로 메시지, 현재는 필요없어서 주석 처리
    }
}