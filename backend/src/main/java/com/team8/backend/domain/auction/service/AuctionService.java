package com.team8.backend.domain.auction.service;

import com.team8.backend.domain.auction.dto.response.AuctionResponse;
import com.team8.backend.domain.auction.entity.AuctionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface AuctionService {
    Page<AuctionResponse> getAuctions(Pageable pageable, String keyword, Long minPrice, Long maxPrice, List<AuctionStatus> statuses);
    AuctionResponse getAuction(Long auctionId);
    List<AuctionResponse> getMyAuctions(Long userId);
    AuctionResponse getMyAuction(Long userId, Long auctionId);
    void closeAuction(Long auctionId);
}