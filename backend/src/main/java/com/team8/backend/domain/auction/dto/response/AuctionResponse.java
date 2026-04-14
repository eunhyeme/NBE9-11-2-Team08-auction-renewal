package com.team8.backend.domain.auction.dto.response;

import com.team8.backend.domain.auction.entity.Auction;
import com.team8.backend.domain.auction.entity.AuctionStatus;

import java.time.LocalDateTime;

/**
 * 8팀 경매 상품 응답 DTO
 * ERD 규격 및 시니어 리뷰어 가이드라인(ID 노출, 낙관적 락 필드 포함) 준수
 */
public record AuctionResponse(
        Long auctionId,
        Long sellerId,           // ERD: seller_id 반영
        String title,
        String imageUrl,         // ERD: image_url 반영
        String description,
        Long startPrice,
        Long currentPrice,
        Long lastBidderId,       // ERD: last_bidder_id 반영 (Nullable)
        Integer bidUnit,
        AuctionStatus status,
        LocalDateTime startedAt,
        LocalDateTime endedAt,
        Long version,            // ERD: version (낙관적 락 상태 확인용)
        LocalDateTime deletedAt  // ERD: deleted_at (Soft Delete 확인용)
) {
    public static AuctionResponse from(Auction auction) {
        return new AuctionResponse(
                auction.getAuctionId(),
                auction.getSeller() != null ? auction.getSeller().getUserId() : null,
                auction.getTitle(),
                auction.getImageUrl(),
                auction.getDescription(),
                auction.getStartPrice(),
                auction.getCurrentPrice(),
                auction.getLastBidder() != null ? auction.getLastBidder().getUserId() : null,
                auction.getBidUnit(),
                auction.getStatus(),
                auction.getStartedAt(),
                auction.getEndedAt(),
                auction.getVersion(),
                auction.getDeletedAt()
        );
    }
}