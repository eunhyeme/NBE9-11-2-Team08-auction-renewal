package com.team8.backend.domain.auction.repository;

import com.team8.backend.domain.auction.entity.Auction;
import com.team8.backend.domain.auction.entity.AuctionStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
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
    
    // 낙관적 락을 적용한 findById
    @Lock(LockModeType.OPTIMISTIC)
    @Query("select a from Auction a where a.auctionId = :id and a.deletedAt is null")
    Optional<Auction> findByIdWithLock(@Param("id") Long id);

   
}
