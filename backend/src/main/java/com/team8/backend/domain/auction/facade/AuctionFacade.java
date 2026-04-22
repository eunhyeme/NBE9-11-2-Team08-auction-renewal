package com.team8.backend.domain.auction.facade;

import com.team8.backend.domain.auction.dto.ImageFile;
import com.team8.backend.domain.auction.dto.request.AuctionCreateRequest;
import com.team8.backend.domain.auction.dto.request.AuctionUpdateRequest;
import com.team8.backend.domain.auction.dto.response.AuctionResponse;

public interface AuctionFacade {
    AuctionResponse createAuction(Long userId, AuctionCreateRequest dto, ImageFile image);
    void deleteAuction(Long userId, Long auctionId);
    void updateAuction(Long userId, Long auctionId, AuctionUpdateRequest request, ImageFile image);
}