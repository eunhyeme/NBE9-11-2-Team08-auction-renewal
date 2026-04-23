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

    //입찰 신청 api
    @Operation(security = @SecurityRequirement(name = "bearerAuth"))
    @PostMapping("/bids")
    public ResponseEntity<ApiResponse<BidResponse>> createBid(
            @RequestBody BidRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails

    ) {
        Long userId = userDetails.getUserId();

        BidResponse bidResponse=bidService.createBid(userId,request);
        return ResponseEntity.ok(ApiResponse.success(bidResponse));

    }

    @GetMapping("/{auctionId}/bids") //상품당 입찰내역
    public ResponseEntity<ApiResponse<List<BidResponse>>> getAuctionBids(
            @PathVariable Long auctionId
    ) {
        return ResponseEntity.ok(ApiResponse.success(bidService.getAuctionBids(auctionId)));
    }
}


