package com.team8.backend.domain.bid.controller;

import com.team8.backend.domain.bid.dto.BidRequest;
import com.team8.backend.domain.bid.dto.BidResponse;
import com.team8.backend.domain.bid.service.BidService;
import com.team8.backend.global.response.ApiResponse;
import com.team8.backend.global.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auctions")
@RequiredArgsConstructor
public class BidController {

    private final BidService bidService;

    @PostMapping("/bids")
    public ResponseEntity<ApiResponse<BidResponse>> createBid(
            @RequestBody BidRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails

    ) {
        Long userId = userDetails.getUserId();

        BidResponse bidResponse=bidService.createBid(userId,request);
        return ResponseEntity.ok(ApiResponse.success(bidResponse));

    }
}


