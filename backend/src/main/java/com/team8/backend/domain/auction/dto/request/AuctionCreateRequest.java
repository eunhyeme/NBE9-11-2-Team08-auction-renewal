package com.team8.backend.domain.auction.dto.request;

import java.time.LocalDateTime;

public record AuctionCreateRequest(
        String title,
        String description,
        Long startPrice,
        Integer bidUnit,
        LocalDateTime startedAt,
        LocalDateTime endedAt
) {
}