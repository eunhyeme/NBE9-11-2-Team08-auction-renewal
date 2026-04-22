package com.team8.backend.domain.auction.service;

import com.team8.backend.domain.auction.entity.Auction;
import com.team8.backend.domain.auction.repository.AuctionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class AuctionDomainService {
    private final AuctionRepository auctionRepository;

    public Auction save(Auction auction) {
        return auctionRepository.save(auction);
    }

    @Transactional(readOnly = true)
    public Auction getById(Long id) {
        return auctionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("경매를 찾을 수 없습니다. ID: " + id));
    }

    public void delete(Auction auction) {
        auctionRepository.delete(auction);
    }
}