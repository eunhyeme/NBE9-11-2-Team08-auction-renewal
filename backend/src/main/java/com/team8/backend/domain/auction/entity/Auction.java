package com.team8.backend.domain.auction.entity;

import com.team8.backend.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "auction")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class Auction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "auction_id")
    private Long auctionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id", nullable = false)
    private User seller;

    @Column(nullable = false)
    private String title;

    @Column(name = "image_url")
    private String imageUrl;

    private String description;

    @Column(name = "start_price", nullable = false)
    private Long startPrice;

    @Column(name = "current_price", nullable = false)
    private Long currentPrice;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "last_bidder_id")
    private User lastBidder;

    @Column(name = "bid_unit", nullable = false)
    private Integer bidUnit;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AuctionStatus status; // ONGOING, WON, UNSOLD, CANCELED

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "ended_at", nullable = false)
    private LocalDateTime endedAt;

    @Version
    @Column(nullable = false)
    private Long version;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    // 비즈니스 로직
    public void placeBid(User bidder, Long bidPrice) {
        this.lastBidder = bidder;
        this.currentPrice = bidPrice;
    }

    public void markWon() { this.status = AuctionStatus.WON; }
    public void markUnsold() { this.status = AuctionStatus.UNSOLD; }
    public void cancel() { this.status = AuctionStatus.CANCELED; }
    public void softDelete(LocalDateTime deletedAt) { this.deletedAt = deletedAt; }
}