package com.team8.backend.domain.auction.repository;

import com.team8.backend.domain.auction.entity.Auction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AuctionRepository extends JpaRepository<Auction, Long> {
    Page<Auction> findAllByDeletedAtIsNull(Pageable pageable);
    Optional<Auction> findByAuctionIdAndDeletedAtIsNull(Long auctionId); // ID로 조회하되, deletedAt이 null인 경우만 반환
}
