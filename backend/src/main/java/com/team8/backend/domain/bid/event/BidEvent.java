package com.team8.backend.domain.bid.event;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class BidEvent {
    private Long auctionId;
    private Long currentPrice;
    private Long bidderId;
}