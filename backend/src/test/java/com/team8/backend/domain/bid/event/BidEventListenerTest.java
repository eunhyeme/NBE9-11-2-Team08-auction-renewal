package com.team8.backend.domain.bid.event;

import com.team8.backend.global.websocket.WSEventPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@SpringBootTest
class BidEventListenerSpringTest {

    @Autowired
    private BidEventListener bidEventListener;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @MockitoBean
    private WSEventPublisher wsEventPublisher;

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
    @DisplayName("BidEvent 수신 → WSEventPublisher.publishBid 정상 호출")
    void handleBidEvent_sendsToCorrectPublisher() {
        bidEventListener.handleBidEvent(sampleEvent);

        verify(wsEventPublisher).publishBid(10L, sampleEvent);
    }

    @Test
    @DisplayName("서로 다른 auctionId는 서로 다른 파라미터로 퍼블리셔에 전달")
    void handleBidEvent_differentAuctionIds_differentCalls() {
        BidEvent event1 = new BidEvent(10L, 2000L, 2L, LocalDateTime.now().toString(), "사용자22");
        BidEvent event2 = new BidEvent(20L, 3000L, 3L, LocalDateTime.now().toString(), "사용자22");

        bidEventListener.handleBidEvent(event1);
        bidEventListener.handleBidEvent(event2);

        verify(wsEventPublisher).publishBid(10L, event1);
        verify(wsEventPublisher).publishBid(20L, event2);
    }

    @Test
    @DisplayName("발행 중 예외 발생 시 핸들러가 예외를 삼키고 정상 종료")
    void handleBidEvent_whenSendFails_doesNotThrow() {
        doThrow(new RuntimeException("STOMP error"))
                .when(wsEventPublisher).publishBid(anyLong(), any());

        org.junit.jupiter.api.Assertions.assertDoesNotThrow(
                () -> bidEventListener.handleBidEvent(sampleEvent)
        );
    }

    @Test
    @DisplayName("입찰 이벤트는 END 로 발행되지 않음 (프론트가 경매 종료로 처리하지 않도록)")
    void handleBidEvent_neverPublishesEnd() {
        bidEventListener.handleBidEvent(sampleEvent);

        verify(wsEventPublisher, never()).publishEnd(anyLong(), any());
    }

    @Test
    @DisplayName("트랜잭션 커밋 후에만 브로드캐스트")
    void publishedAfterCommit() {
        transactionTemplate.executeWithoutResult(status -> {
            eventPublisher.publishEvent(sampleEvent);
            // 커밋 전에는 아직 발행되지 않음
            verify(wsEventPublisher, never()).publishBid(anyLong(), any());
        });

        verify(wsEventPublisher).publishBid(10L, sampleEvent);
    }

    @Test
    @DisplayName("트랜잭션 롤백 시 브로드캐스트하지 않음")
    void notPublishedOnRollback() {
        transactionTemplate.executeWithoutResult(status -> {
            eventPublisher.publishEvent(sampleEvent);
            status.setRollbackOnly();
        });

        verify(wsEventPublisher, never()).publishBid(anyLong(), any());
    }
}
