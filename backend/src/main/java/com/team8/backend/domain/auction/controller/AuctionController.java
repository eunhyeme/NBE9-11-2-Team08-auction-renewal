package com.team8.backend.domain.auction.controller;

import com.team8.backend.domain.auction.dto.ImageFile;
import com.team8.backend.domain.auction.dto.request.AuctionCreateRequest;
import com.team8.backend.domain.auction.dto.request.AuctionUpdateRequest;
import com.team8.backend.domain.auction.dto.response.AuctionResponse;
import com.team8.backend.domain.auction.entity.AuctionStatus;
import com.team8.backend.domain.auction.facade.AuctionFacade;
import com.team8.backend.domain.auction.service.AuctionService;
import com.team8.backend.global.exception.CustomException;
import com.team8.backend.global.exception.ErrorCode;
import com.team8.backend.global.response.ApiResponse;
import com.team8.backend.global.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auctions")
public class AuctionController {

    // 이 컨트롤러는 공개 경매 API와 판매자 전용 마이페이지 API를 함께 담당합니다.
    private final AuctionService auctionService;
    private final AuctionFacade auctionFacade;

    /**
     * 경매 목록 조회 (비인증 허용)
     * GET /api/auctions?page=0&size=10&sort=createdAt,desc
     */
    @GetMapping
    public ResponseEntity<ApiResponse<Page<AuctionResponse>>> getAuctions(
            @PageableDefault(size = 10, sort = "startedAt", direction = Sort.Direction.DESC) Pageable pageable,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long minPrice,
            @RequestParam(required = false) Long maxPrice,
            @RequestParam(required = false) List<AuctionStatus> statuses
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                auctionService.getAuctions(pageable, keyword, minPrice, maxPrice, statuses)));
    }

    /**
     * 경매 상세 조회 (비인증 허용)
     * GET /api/auctions/{auctionId}
     */
    @GetMapping("/{auctionId}")
    public ResponseEntity<ApiResponse<AuctionResponse>> getAuction(@PathVariable Long auctionId) {
        return ResponseEntity.ok(ApiResponse.success(auctionService.getAuction(auctionId)));
    }

    @Operation(security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<List<AuctionResponse>>> getMyAuctions(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        //이 API는 인증된 판매자 본인 기준으로 조회합니다.
        return ResponseEntity.ok(ApiResponse.success(auctionService.getMyAuctions(userDetails.getUserId())));
    }

    @Operation(security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping("/me/{auctionId}")
    public ResponseEntity<ApiResponse<AuctionResponse>> getMyAuction(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long auctionId
    ) {
        // 이 상세 조회 API는 두 조건을 모두 만족해야 합니다:
        // 경매 ID가 일치하고, 현재 사용자가 해당 경매의 판매자여야 합니다.
        return ResponseEntity.ok(ApiResponse.success(auctionService.getMyAuction(userDetails.getUserId(), auctionId)));
    }

    /**
     * 경매 상품 등록
     */
    @Operation(security = @SecurityRequirement(name = "bearerAuth"))
    @PostMapping(consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    public ResponseEntity<ApiResponse<?>> createAuction(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestPart("request") @Valid AuctionCreateRequest request,
            BindingResult bindingResult,
            @RequestPart(value = "image", required = true) MultipartFile image // 💡 required = true로 변경
    ) {
        if (bindingResult.hasErrors()) {
            return handleValidationErrors(bindingResult);
        }

        ImageFile imageFile = toImageFile(image);
        AuctionResponse response = auctionFacade.createAuction(userDetails.getUserId(), request, imageFile);

        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    /**
     * 경매 철회 (Soft Delete)
     * DELETE /api/auctions/{auctionId}
     */
    @Operation(security = @SecurityRequirement(name = "bearerAuth"))
    @DeleteMapping("/{auctionId}")
    public ResponseEntity<ApiResponse<?>> deleteAuction(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long auctionId
    ) {
        // 💡 변경된 부분: 컨트롤러는 파사드에게 삭제를 '명령'만 합니다.
        // 내부에서 권한 체크, 입찰자 체크, 이미지 삭제, DB 삭제가 한 번에 일어납니다.
        auctionFacade.deleteAuction(userDetails.getUserId(), auctionId);

        return ResponseEntity.ok(ApiResponse.success("경매가 성공적으로 삭제되었습니다."));
    }

    /**
     * 경매 정보 수정
     * PATCH /api/auctions/{auctionId}
     */
    @Operation(security = @SecurityRequirement(name = "bearerAuth"))
    @PatchMapping(value = "/{auctionId}", consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    public ResponseEntity<ApiResponse<?>> updateAuction(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long auctionId,
            @RequestPart(value = "request", required = false) @Valid AuctionUpdateRequest request,
            BindingResult bindingResult,
            @RequestPart(value = "image", required = false) MultipartFile image
    ) {
        if (bindingResult.hasErrors()) {
            return handleValidationErrors(bindingResult);
        }

        // 💡 MultipartFile -> ImageFile 변환 후 파사드 호출
        ImageFile imageFile = toImageFile(image);
        auctionFacade.updateAuction(userDetails.getUserId(), auctionId, request, imageFile);

        return ResponseEntity.ok(ApiResponse.success(null));
    }

    // 에러 처리 중복 제거를 위한 헬퍼 메소드
    private ResponseEntity<ApiResponse<?>> handleValidationErrors(BindingResult bindingResult) {
        Map<String, String> errors = new HashMap<>();
        for (FieldError error : bindingResult.getFieldErrors()) {
            errors.put(error.getField(), error.getDefaultMessage());
        }
        // 특정 필드 에러에 따른 ErrorCode 세분화
        ErrorCode code = ErrorCode.VALIDATION_ERROR;
        // 입찰 단위 관련 에러가 있는지 체크
        if (bindingResult.hasFieldErrors("bidUnitMultipleOf100")) {
            code = ErrorCode.BID_UNIT_NOT_MULTIPLE;
        }
        // 추가로 다른 특정 에러(예: 제목 누락 등)에 대한 세분화가 필요하면 여기에 추가
        return ResponseEntity.status(code.getHttpStatus())
                .body(new ApiResponse<>(code.getCode(), code.getMessage(), errors));
    }

    /**
     * 💡 MultipartFile을 ImageFile DTO로 변환하는 헬퍼 메서드
     */
    private ImageFile toImageFile(MultipartFile file) {
        if (file == null || file.isEmpty()) return null;
        try {
            return new ImageFile(
                    file.getInputStream(),
                    file.getOriginalFilename(),
                    file.getContentType(),
                    file.getSize()
            );
        } catch (java.io.IOException e) {
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }
}
