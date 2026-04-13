package com.team8.backend.domain.auction.repository;

import com.team8.backend.domain.auction.entity.Auction;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuctionRepository extends JpaRepository<Auction, Long> {
}
