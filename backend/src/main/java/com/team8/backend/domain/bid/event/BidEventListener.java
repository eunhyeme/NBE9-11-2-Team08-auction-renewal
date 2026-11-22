package com.team8.backend.domain.bid.event;

import com.team8.backend.global.websocket.WSEventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
@RequiredArgsConstructor
public class BidEventListener {

    private static final Logger log = LoggerFactory.getLogger(BidEventListener.class);

    private final WSEventPublisher WSEventPublisher;

    // 입찰 트랜잭션이 커밋된 뒤에만 브로드캐스트 (롤백된 입찰가가 퍼지지 않도록)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleBidEvent(BidEvent event) {

        // 1. 이벤트 들어왔는지 확인
        log.info("[BidEventListener] 이벤트 수신 : {} ",event.getBidedAt());

        try {
            // 확인 후 발행
            WSEventPublisher.publishBid(event.getAuctionId(), event);
        } catch (Exception e) {
            // 4. 전송 실패 로그
            log.error("[BidEventListener] 메시지 전송 실패", e);
        }
    }
}