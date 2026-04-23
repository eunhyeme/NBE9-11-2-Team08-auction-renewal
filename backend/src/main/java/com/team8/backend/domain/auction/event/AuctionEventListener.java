package com.team8.backend.domain.auction.event;

import com.team8.backend.global.websocket.WSEventPublisher;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AuctionEventListener {

    private static final Logger log = LoggerFactory.getLogger(AuctionEventListener.class);

    private final WSEventPublisher WSEventPublisher;

    @EventListener
    public void handleAuctionEvent(AuctionEvent event) {

        // 이벤트 들어왔는지 확인
        log.info("[AuctionEventListener] 이벤트 수신-종료이벤트 : {} ",event.getAuctionId());

        try {
            // 확인 후 발행
            WSEventPublisher.publishEnd(event.getAuctionId(), event);
        } catch (Exception e) {
            // 4. 전송 실패 로그
            log.error("[BidEventListener] 메시지 전송 실패", e);
        }
    }
}