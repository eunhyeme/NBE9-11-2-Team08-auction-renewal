package com.team8.backend.domain.auction.controller;

import com.team8.backend.domain.auction.dto.request.AuctionCreateRequest;
import com.team8.backend.domain.auction.dto.request.AuctionUpdateRequest;
import com.team8.backend.domain.auction.dto.response.AuctionResponse;
import com.team8.backend.domain.auction.service.AuctionService;
import com.team8.backend.global.exception.ErrorCode;
import com.team8.backend.global.response.ApiResponse;
import com.team8.backend.global.security.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

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
    public ResponseEntity<ApiResponse<?>> createAuction(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody AuctionCreateRequest request,
            BindingResult bindingResult
    ) {
        if (bindingResult.hasErrors()) {
            Map<String, String> errors = new HashMap<>();
            boolean isBidUnitError = false;
            for (FieldError error : bindingResult.getFieldErrors()) {
                errors.put(error.getField(), error.getDefaultMessage());
                // 입찰 단위 검증 메서드명이나 필드명으로 체크
                if ("bidUnitMultipleOf100".equals(error.getField())) {
                    isBidUnitError = true;
                }
            }
            // 입찰 단위 에러라면 전용 코드를, 아니면 일반 검증 코드를 씁니다.
            ErrorCode code = isBidUnitError ? ErrorCode.BID_UNIT_NOT_MULTIPLE : ErrorCode.VALIDATION_ERROR;
            return ResponseEntity.status(code.getHttpStatus())
                    .body(new ApiResponse<>(code.getCode(), code.getMessage(), errors));
        }

        AuctionResponse response = auctionService.createAuction(userDetails.getUserId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    /**
     * 경매 철회 (Soft Delete)
     * DELETE /api/auctions/{auctionId}
     */
    @DeleteMapping("/{auctionId}")
    public ResponseEntity<ApiResponse<Void>> withdrawAuction(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long auctionId
    ) {
        auctionService.withdrawAuction(userDetails.getUserId(), auctionId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    /**
     * 경매 정보 수정
     * PATCH /api/auctions/{auctionId}
     */
    @PatchMapping("/{auctionId}")
    public ResponseEntity<ApiResponse<?>> updateAuction(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long auctionId,
            @Valid @RequestBody AuctionUpdateRequest request,
            BindingResult bindingResult
    ) {
        if (bindingResult.hasErrors()) {
            Map<String, String> errors = new HashMap<>();
            boolean isBidUnitError = false;
            for (FieldError error : bindingResult.getFieldErrors()) {
                errors.put(error.getField(), error.getDefaultMessage());
                // 입찰 단위 검증 메서드명이나 필드명으로 체크
                if ("bidUnitMultipleOf100".equals(error.getField())) {
                    isBidUnitError = true;
                }
            }
            // 입찰 단위 에러라면 전용 코드를, 아니면 일반 검증 코드를 씁니다.
            ErrorCode code = isBidUnitError ? ErrorCode.BID_UNIT_NOT_MULTIPLE : ErrorCode.VALIDATION_ERROR;
            return ResponseEntity.status(code.getHttpStatus())
                    .body(new ApiResponse<>(code.getCode(), code.getMessage(), errors));
        }

        auctionService.updateAuction(userDetails.getUserId(), auctionId, request);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
