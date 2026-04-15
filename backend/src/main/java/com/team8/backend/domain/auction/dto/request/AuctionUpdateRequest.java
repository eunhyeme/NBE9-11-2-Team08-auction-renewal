package com.team8.backend.domain.auction.dto.request;

public record AuctionUpdateRequest(
        String title,
        String description,
        String imageUrl,
        Long startPrice,
        Integer bidUnit,
        Long durationInDays
) {}