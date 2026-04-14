package com.team8.backend.domain.bid.controller;

import com.team8.backend.domain.bid.dto.BidRequest;
import com.team8.backend.domain.bid.dto.BidResponse;
import com.team8.backend.domain.bid.service.BidService;
import com.team8.backend.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
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
    public ResponseEntity<ApiResponse<BidResponse>> placeBid(@RequestBody BidRequest request) {
        BidResponse bidResponse=bidService.createBid(request);
        return ResponseEntity.ok(ApiResponse.success(bidResponse));

    }
}


