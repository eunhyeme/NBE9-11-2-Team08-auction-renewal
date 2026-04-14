package com.team8.backend.domain.auction.controller;

import com.team8.backend.domain.auction.dto.request.AuctionCreateRequest;
import com.team8.backend.domain.auction.dto.response.AuctionResponse;
import com.team8.backend.domain.auction.service.AuctionService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auctions")
public class AuctionController {

    private final AuctionService auctionService;

    /**
     * 경매 목록 조회 (비인증 허용)
     * GET /api/auctions?page=0&size=10&sort=createdAt,desc
     */
    @GetMapping
    public Page<AuctionResponse> getAuctions(
            @PageableDefault(size = 10, sort = "startedAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return auctionService.getAuctions(pageable);
    }

    /**
     * 경매 상품 등록
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AuctionResponse createAuction(
            @RequestHeader("X-USER-ID") Long userId,
            @RequestBody AuctionCreateRequest request
    ) {
        return auctionService.createAuction(userId, request);
    }
}