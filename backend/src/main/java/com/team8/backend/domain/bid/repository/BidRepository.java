package com.team8.backend.domain.bid.repository;

import com.team8.backend.domain.bid.entity.Bid;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface BidRepository extends JpaRepository<Bid, Long> {

    @Query("""
            SELECT b FROM Bid b
            JOIN FETCH b.auction a
            JOIN FETCH a.seller
            LEFT JOIN FETCH a.lastBidder
            WHERE b.bidder.userId = :userId
              AND b.bidId = (
                SELECT MAX(b2.bidId)
                FROM Bid b2
                WHERE b2.bidder.userId = :userId
                  AND b2.auction.auctionId = b.auction.auctionId
                  AND b2.bidedAt = (
                    SELECT MAX(b3.bidedAt)
                    FROM Bid b3
                    WHERE b3.bidder.userId = :userId
                      AND b3.auction.auctionId = b.auction.auctionId
                  )
              )
            ORDER BY b.bidedAt DESC, b.bidId DESC
            """)
    List<Bid> findLatestByBidderUserId(@Param("userId") Long userId);

    @Query("""
            SELECT b FROM Bid b
            JOIN FETCH b.auction a
            JOIN FETCH a.seller
            LEFT JOIN FETCH a.lastBidder
            WHERE b.bidder.userId = :userId
              AND a.auctionId = :auctionId
            ORDER BY b.bidedAt DESC, b.bidId DESC
            """)
    List<Bid> findAllByBidderUserIdAndAuctionIdOrderByBidedAtDesc(
            @Param("userId") Long userId,
            @Param("auctionId") Long auctionId
    );

    List<Bid> findByAuction_AuctionIdOrderByBidedAtDesc(Long auctionId);

}
