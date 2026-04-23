package com.team8.backend.domain.auction.repository;

import com.team8.backend.domain.auction.entity.Auction;
import com.team8.backend.domain.auction.entity.AuctionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface AuctionRepository extends JpaRepository<Auction, Long>, JpaSpecificationExecutor<Auction> {
    Page<Auction> findAllByDeletedAtIsNull(Pageable pageable);

    // 판매자 기준으로 마이페이지 경매 이력을 조회하는 쿼리입니다.
    List<Auction> findAllBySeller_UserIdAndDeletedAtIsNullOrderByStartedAtDesc(Long userId);

    // 판매자 기준으로 마이페이지 경매 상세를 조회하는 쿼리입니다.
    Optional<Auction> findByAuctionIdAndSeller_UserIdAndDeletedAtIsNull(Long auctionId, Long userId);

    Optional<Auction> findByAuctionIdAndDeletedAtIsNull(Long auctionId); // ID로 조회하되, deletedAt이 null인 경우만 반환

    // 상태가 ONGOING이고 종료 시간이 현재 시간 보다 이전인 경매 조회
    List<Auction> findByStatusAndEndedAtBeforeAndDeletedAtIsNull(AuctionStatus status, LocalDateTime now);
    
    // [청크] Slice를 사용하여 마감 대상 경매를 효율적으로 조회
    Slice<Auction> findByStatusAndEndedAtBeforeAndDeletedAtIsNull(
            AuctionStatus status,
            LocalDateTime now,
            Pageable pageable
    );

    // 조건부 원자 UPDATE: newPrice > current_price 이고 bid_unit 충족 시에만 갱신
    // clearAutomatically=true → 실행 후 1차 캐시 무효화 (이후 재조회 시 DB 값 반영)
    // flushAutomatically = true -> update 쿼리가 항상 최신 영속성 상태 기준으로 실행됨
    // 조건부 원자 UPDATE: 입찰 갱신 + 종료 5분 이내이면 5분 연장을 단일 쿼리로 처리
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
            UPDATE auction
            SET current_price  = :newPrice,
                last_bidder_id = :bidderId,
                version        = version + 1,
                ended_at       = CASE
                                   WHEN ended_at < :nowPlus5
                                   THEN :nowPlus5
                                   ELSE ended_at
                                 END
            WHERE auction_id   = :auctionId
              AND :bidderId    != seller_id
              AND :newPrice    > current_price
              AND (:newPrice - current_price) >= bid_unit
              AND status       = 'ONGOING'
              AND ended_at     > :now
              AND deleted_at   IS NULL
            """, nativeQuery = true)
    int atomicBidUpdate(@Param("auctionId") Long auctionId,
                        @Param("newPrice")   Long newPrice,
                        @Param("bidderId")   Long bidderId,
                        @Param("now")        LocalDateTime now,
                        @Param("nowPlus5")   LocalDateTime nowPlus5);
}
