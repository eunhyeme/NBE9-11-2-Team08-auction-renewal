package com.team8.backend.domain.bid.dto;

import java.time.LocalDateTime;

public record BidResponse(
        Long auctionId,
        Long bidPrice,
        Long bidderId,
        LocalDateTime bidedAt
) {}
