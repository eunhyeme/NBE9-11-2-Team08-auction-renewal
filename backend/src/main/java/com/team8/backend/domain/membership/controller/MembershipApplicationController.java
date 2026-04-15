package com.team8.backend.domain.membership.controller;

import com.team8.backend.domain.membership.dto.request.MembershipReviewRequest;
import com.team8.backend.domain.membership.dto.response.MembershipApplyResponse;
import com.team8.backend.domain.membership.dto.response.MembershipApplicationSummaryResponse;
import com.team8.backend.domain.membership.service.MembershipApplicationService;
import com.team8.backend.global.response.ApiResponse;
import com.team8.backend.global.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/membership")
@RequiredArgsConstructor
public class MembershipApplicationController {

    private final MembershipApplicationService membershipApplicationService;

    @PreAuthorize("hasRole('ADMIN')")
    @Operation(security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping
    public ResponseEntity<ApiResponse<List<MembershipApplicationSummaryResponse>>> getApplications(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {

        List<MembershipApplicationSummaryResponse> response =
                membershipApplicationService.getApplications(userDetails);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PreAuthorize("hasRole('USER')")
    @Operation(security = @SecurityRequirement(name = "bearerAuth"))
    @PostMapping("/apply")
    public ResponseEntity<ApiResponse<MembershipApplyResponse>> apply(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        MembershipApplyResponse response = membershipApplicationService.apply(userDetails);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Operation(security = @SecurityRequirement(name = "bearerAuth"))
    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<MembershipApplicationSummaryResponse>> review(
            @PathVariable Long id,
            @Valid @RequestBody MembershipReviewRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        MembershipApplicationSummaryResponse response =
                membershipApplicationService.review(id, request, userDetails);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
