package com.team8.backend.domain.bid.controller;

import com.team8.backend.domain.bid.dto.BidRequest;
import com.team8.backend.domain.bid.dto.BidResponse;
import com.team8.backend.domain.bid.service.BidService;
import com.team8.backend.global.response.ApiResponse;
import com.team8.backend.global.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/auctions")
@RequiredArgsConstructor
public class BidController {

    private final BidService bidService;

    @Operation(summary = "입찰 (기존)", security = @SecurityRequirement(name = "bearerAuth"))
    @PostMapping("/bids")
    public ResponseEntity<ApiResponse<BidResponse>> createBid(
            @RequestBody BidRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        BidResponse bidResponse = bidService.createBid(userDetails.getUserId(), request);
        return ResponseEntity.ok(ApiResponse.success(bidResponse));
    }

    @Operation(summary = "입찰 (원자 UPDATE)", security = @SecurityRequirement(name = "bearerAuth"))
    @PostMapping("/bids/atomic")
    public ResponseEntity<ApiResponse<BidResponse>> createBidAtomic(
            @RequestBody BidRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        BidResponse bidResponse = bidService.createBidAtomic(userDetails.getUserId(), request);
        return ResponseEntity.ok(ApiResponse.success(bidResponse));
    }

    @GetMapping("/{auctionId}/bids") //상품당 입찰내역
    public ResponseEntity<ApiResponse<List<BidResponse>>> getAuctionBids(
            @PathVariable Long auctionId
    ) {
        return ResponseEntity.ok(ApiResponse.success(bidService.getAuctionBids(auctionId)));
    }
}


