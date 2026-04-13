package com.team8.backend.domain.auction.dto.response;

import com.team8.backend.domain.auction.entity.Auction;
import com.team8.backend.domain.auction.entity.AuctionStatus;

import java.time.LocalDateTime;

public record AuctionResponse(
        Long auctionId,
        String sellerNickname, // User 엔티티의 nickname 활용
        String title,
        String description,
        String imageUrl,
        Long startPrice,
        Long currentPrice,
        String lastBidderNickname, // 입찰자가 없을 경우 null
        Integer bidUnit,
        AuctionStatus status,
        LocalDateTime startedAt,
        LocalDateTime endedAt
) {
    public static AuctionResponse from(Auction auction) {
        return new AuctionResponse(
                auction.getAuctionId(),
                auction.getSeller().getNickname(), // 판매자 닉네임
                auction.getTitle(),
                auction.getDescription(),
                auction.getImageUrl(),
                auction.getStartPrice(),
                auction.getCurrentPrice(),
                auction.getLastBidder() != null ? auction.getLastBidder().getNickname() : null, // 입찰자 없으면 null 처리
                auction.getBidUnit(),
                auction.getStatus(),
                auction.getStartedAt(),
                auction.getEndedAt()
        );
    }
}