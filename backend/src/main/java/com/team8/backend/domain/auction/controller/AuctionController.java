package com.team8.backend.domain.auction.controller;

import com.team8.backend.domain.auction.dto.request.AuctionCreateRequest;
import com.team8.backend.domain.auction.dto.response.AuctionResponse;
import com.team8.backend.domain.auction.service.AuctionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auctions")
public class AuctionController {

    private final AuctionService auctionService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED) // 성공 시 201 Created 반환
    public AuctionResponse createAuction(
            @RequestHeader("X-USER-ID") Long userId, // 헤더로 유저 ID를 받는 임시 방식
            @RequestBody AuctionCreateRequest request
    ) {
        return auctionService.createAuction(userId, request);
    }
}