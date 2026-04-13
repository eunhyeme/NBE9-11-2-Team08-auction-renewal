package com.team8.backend.domain.bid.repository;

import com.team8.backend.domain.bid.entity.Bid;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BidRepository extends JpaRepository<Bid, Long> {
}
