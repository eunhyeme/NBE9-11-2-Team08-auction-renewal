package com.team8.backend.domain.bid.entity;

import com.team8.backend.domain.auction.entity.Auction;
import com.team8.backend.domain.user.entity.User;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "bid")
public class Bid {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "bid_id")
    private Long bidId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bidder_id", nullable = false)
    private User bidder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "auction_id", nullable = false)
    private Auction auction;

    @Column(name = "bid_price", nullable = false)
    private Long bidPrice;

    @Column(name = "bided_at", nullable = false)
    private LocalDateTime bidedAt;

    protected Bid() {}

    public Bid(User bidder, Auction auction, Long bidPrice, LocalDateTime bidedAt) {
        this.bidder = bidder;
        this.auction = auction;
        this.bidPrice = bidPrice;
        this.bidedAt = bidedAt;
    }

    public Long getBidId() { return bidId; }
    public User getBidder() { return bidder; }
    public Auction getAuction() { return auction; }
    public Long getBidPrice() { return bidPrice; }
    public LocalDateTime getBidedAt() { return bidedAt; }
}
