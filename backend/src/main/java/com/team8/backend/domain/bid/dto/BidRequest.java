package com.team8.backend.domain.bid.dto;

public record BidRequest(
        Long auctionId,
        Long bidderId,
        Long bidPrice
) {}