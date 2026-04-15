package com.team8.backend.websocket.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer { // 웹소켓 활성화 + STOMP 메시지브로커 디폴트사용

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic"); // 클라이언트가 서버(메시지브로커)에 구독할 주소 + 예) /topic/auction/1
        config.setApplicationDestinationPrefixes("/app"); // 발행
            // 클라이언트-> 서버로 메시지
    }

    // 웹소켓 엔드포인트 등록
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")                 //ws://localhost:8080/ws
                .setAllowedOriginPatterns("*");     //일단 test동안 CORS 허용
//                .withSockJS();                    //postman은 sockJS가 아닌 순수 웹소켓 테스트만 지원해서 백엔드 개발동안은 꺼둠
    }
}