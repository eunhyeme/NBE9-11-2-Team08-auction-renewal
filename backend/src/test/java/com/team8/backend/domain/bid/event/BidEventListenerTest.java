package com.team8.backend.domain.bid.event;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@SpringBootTest
class BidEventListenerSpringTest {

    // 실제 빈으로 주입 (스프링이 관리하는 BidEventListener)
    @Autowired
    private BidEventListener bidEventListener;

    // SimpMessagingTemplate만 Mock으로 교체 (실제 WebSocket 브로커 불필요)
    @MockitoBean
    private SimpMessagingTemplate messagingTemplate;

    private BidEvent sampleEvent;

    @BeforeEach
    void setUp() {
        sampleEvent = new BidEvent(
                10L,
                2000L,
                2L,
                LocalDateTime.now().toString(),
                "사용자22"
        );
    }

    @Test
    @DisplayName("BidEvent 수신 → 올바른 destination으로 WebSocket 메시지 전송")
    void handleBidEvent_sendsToCorrectDestination() {
        bidEventListener.handleBidEvent(sampleEvent);

        ArgumentCaptor<String> destCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<BidEvent> payloadCaptor = ArgumentCaptor.forClass(BidEvent.class);

        verify(messagingTemplate).convertAndSend(destCaptor.capture(), payloadCaptor.capture());

        assertThat(destCaptor.getValue()).isEqualTo("/topic/auction/10");
        assertThat(payloadCaptor.getValue().getBidPrice()).isEqualTo(2000L);
        assertThat(payloadCaptor.getValue().getBidderId()).isEqualTo(2L);
    }

    @Test
    @DisplayName("서로 다른 auctionId는 서로 다른 topic으로 전송")
    void handleBidEvent_differentAuctionIds_differentDestinations() {
        BidEvent event1 = new BidEvent(10L, 2000L, 2L, LocalDateTime.now().toString(), "사용자22");
        BidEvent event2 = new BidEvent(20L, 3000L, 3L, LocalDateTime.now().toString(), "사용자22");

        bidEventListener.handleBidEvent(event1);
        bidEventListener.handleBidEvent(event2);

        verify(messagingTemplate).convertAndSend(eq("/topic/auction/10"), eq(event1));
        verify(messagingTemplate).convertAndSend(eq("/topic/auction/20"), eq(event2));
    }

    @Test
    @DisplayName("messagingTemplate 예외 발생 시 핸들러가 예외를 삼키고 정상 종료")
    void handleBidEvent_whenSendFails_doesNotThrow() {
        doThrow(new RuntimeException("STOMP error"))
                .when(messagingTemplate).convertAndSend(any(String.class), any(Object.class));

        org.junit.jupiter.api.Assertions.assertDoesNotThrow(
                () -> bidEventListener.handleBidEvent(sampleEvent)
        );
    }
}
