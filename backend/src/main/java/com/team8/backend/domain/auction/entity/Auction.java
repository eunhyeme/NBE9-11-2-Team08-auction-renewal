package com.team8.backend.domain.auction.entity;

import com.team8.backend.domain.auction.dto.request.AuctionUpdateRequest;
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
    @Builder.Default // 💡 빌더 사용 시에도 0L로 초기화해줍니다.
    @Column(name = "version", nullable = false)
    private Long version = 0L;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    // 비즈니스 로직
    public void placeBid(User bidder, Long bidPrice) {
        this.lastBidder = bidder;
        this.currentPrice = bidPrice;
    }

    public void withdraw() {
        this.deletedAt = LocalDateTime.now();
        this.status = AuctionStatus.CANCELED;
    }

    public void update(AuctionUpdateRequest request) {
        this.title = request.title();
        this.description = request.description();
        this.imageUrl = request.imageUrl(); // 이제 DTO 내부의 URL을 사용합니다.
        this.startPrice = request.startPrice();
        this.bidUnit = request.bidUnit();

        // 현재 경매 가격은 시작가로 초기화
        this.currentPrice = request.startPrice();

        if (request.durationInDays() != null) {
            this.endedAt = this.startedAt.plusDays(request.durationInDays());
        }
    }

    public void extendEndTime(long minutes) {
        this.endedAt = this.endedAt.plusMinutes(minutes);
    }

    public void markWon() { this.status = AuctionStatus.WON; }
    public void markUnsold() { this.status = AuctionStatus.UNSOLD; }
    public void cancel() { this.status = AuctionStatus.CANCELED; }
    public void softDelete(LocalDateTime deletedAt) { this.deletedAt = deletedAt; }

    public void close() {
        // 이미 마감된 경우 상태 변경 방지
        if (this.status != AuctionStatus.ONGOING || this.endedAt.isAfter(LocalDateTime.now())) {
            return;
        }

        // 낙찰자 존재 여부에 따라 낙찰(WON) or 유찰(UNSOLD)
        if (this.lastBidder != null) {
            markWon();
        } else {
            markUnsold();
        }
    }
}
