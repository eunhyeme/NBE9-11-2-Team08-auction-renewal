package com.team8.backend.domain.auction.controller;

import com.team8.backend.domain.auction.dto.request.AuctionCreateRequest;
import com.team8.backend.domain.auction.dto.response.AuctionResponse;
import com.team8.backend.domain.auction.service.AuctionService;
import com.team8.backend.global.response.ApiResponse;
import com.team8.backend.global.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
    public ResponseEntity<ApiResponse<Page<AuctionResponse>>> getAuctions(
            @PageableDefault(size = 10, sort = "startedAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(ApiResponse.success(auctionService.getAuctions(pageable)));
    }

    /**
     * 경매 상세 조회 (비인증 허용)
     * GET /api/auctions/{auctionId}
     */
    @GetMapping("/{auctionId}")
    public ResponseEntity<ApiResponse<AuctionResponse>> getAuction(@PathVariable Long auctionId) {
        return ResponseEntity.ok(ApiResponse.success(auctionService.getAuction(auctionId)));
    }

    /**
     * 경매 상품 등록
     */
    @PostMapping
    public ResponseEntity<ApiResponse<AuctionResponse>> createAuction(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody AuctionCreateRequest request
    ) {
        AuctionResponse response = auctionService.createAuction(userDetails.getUserId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }
}