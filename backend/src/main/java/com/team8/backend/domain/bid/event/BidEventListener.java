package com.team8.backend.domain.bid.event;

import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
@RequiredArgsConstructor
public class BidEventListener {

    private static final Logger log = LoggerFactory.getLogger(BidEventListener.class);

    private final SimpMessagingTemplate messagingTemplate;

    @EventListener
    public void handleBidEvent(BidEvent event) {

        // 1. 이벤트 들어왔는지 확인
        log.info("[BidEventListener] 이벤트 수신 : {} ",event.getBidedAt());

        String destination = "/topic/auction/" + event.getAuctionId();

        // 2. 어디로 보내는지 확인
        log.info("[BidEventListener] 메시지 전송 시도 - destination: {}", destination);

        try {
            messagingTemplate.convertAndSend(destination, event);

            // 3. 전송 성공 로그
            log.info("[BidEventListener] 메시지 전송 성공");

        } catch (Exception e) {
            // 4. 전송 실패 로그
            log.error("[BidEventListener] 메시지 전송 실패", e);
        }
    }
}