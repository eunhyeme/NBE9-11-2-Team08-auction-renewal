package com.team8.backend.domain.bid.dto;

public record BidResponse(
        Long auctionId,
        Long currentPrice,
        Long bidderId
) {}
