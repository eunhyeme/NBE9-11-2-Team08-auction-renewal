package com.team8.backend.domain.bid.controller;

import com.team8.backend.domain.bid.dto.MyBidResponse;
import com.team8.backend.domain.bid.service.BidService;
import com.team8.backend.global.response.ApiResponse;
import com.team8.backend.global.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/bids")
@RequiredArgsConstructor
public class BidQueryController {

    private final BidService bidService;

    @Operation(security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping("/me")
    public ApiResponse<List<MyBidResponse>> getMyBids(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ApiResponse.success(bidService.getMyBids(userDetails.getUserId()));
    }

    @Operation(security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping("/me/{auctionId}")
    public ApiResponse<List<MyBidResponse>> getMyBidHistory(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long auctionId) {
        return ApiResponse.success(bidService.getMyBidHistory(userDetails.getUserId(), auctionId));
    }
}
