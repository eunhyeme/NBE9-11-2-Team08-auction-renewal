package com.team8.backend.domain.bid.dto;

import com.team8.backend.domain.auction.entity.Auction;
import com.team8.backend.domain.auction.entity.AuctionStatus;
import com.team8.backend.domain.bid.entity.Bid;
import com.team8.backend.domain.user.entity.User;

import java.time.LocalDateTime;

public record MyBidResponse(
        Long bidId,
        Long auctionId,
        String auctionTitle,
        String auctionImageUrl,
        Long myBidPrice,
        Long currentPrice,
        AuctionStatus auctionStatus,
        Long sellerId,
        String sellerNickname,
        LocalDateTime bidAt,
        LocalDateTime auctionEndedAt,
        Long lastBidderId
) {
    public static MyBidResponse from(Bid bid) {
        Auction auction = bid.getAuction();
        User seller = auction.getSeller();
        User lastBidder = auction.getLastBidder();

        return new MyBidResponse(
                bid.getBidId(),
                auction.getAuctionId(),
                auction.getTitle(),
                auction.getImageUrl(),
                bid.getBidPrice(),
                auction.getCurrentPrice(),
                auction.getStatus(),
                seller.getUserId(),
                seller.getNickname(),
                bid.getBidedAt(),
                auction.getEndedAt(),
                lastBidder != null ? lastBidder.getUserId() : null
        );
    }
}
