package com.team8.backend.global.websocket;

import com.team8.backend.global.event.AuctionEvent;
import com.team8.backend.global.event.AuctionEventType;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class WSEventPublisher {


    private final SimpMessagingTemplate messagingTemplate;

    public void publishBid(Long auctionId, Object bidResponse) {
        AuctionEvent<Object> event =
                new AuctionEvent<>(AuctionEventType.BID.name(), bidResponse);

        messagingTemplate.convertAndSend(
                "/topic/auction/" + auctionId,
                event
        );
    }

    public void publishEnd(Long auctionId, Object endResponse) {
        AuctionEvent<Object> event =
                new AuctionEvent<>(AuctionEventType.END.name(), endResponse);

        messagingTemplate.convertAndSend(
                "/topic/auction/" + auctionId,
                event
        );
    }
}