package com.team8.backend.domain.order.controller;

import com.team8.backend.domain.order.dto.request.OrderAddressRequest;
import com.team8.backend.domain.order.dto.request.OrderTrackingRequest;
import com.team8.backend.domain.order.dto.response.OrderResponse;
import com.team8.backend.domain.order.entity.OrderFilter;
import com.team8.backend.domain.order.entity.OrderHistoryRole;
import com.team8.backend.domain.order.facade.OrderFacade;
import com.team8.backend.domain.order.service.OrderService;
import com.team8.backend.global.response.ApiResponse;
import com.team8.backend.global.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final OrderFacade orderFacade;

    // [참여자] 주문 상세 조회 /api/orders/{orderId}
    @Operation(security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping("/{orderId}")
    public ApiResponse<OrderResponse> getOrder(
            @PathVariable Long orderId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ApiResponse.success(orderFacade.getOrder(orderId, userDetails.getUserId()));
    }

    // [구매자] 배송 정보 입력 /api/orders/{orderId}/shipping
    @Operation(security = @SecurityRequirement(name = "bearerAuth"))
    @PostMapping("/{orderId}/shipping")
    public ApiResponse<Void> updateAddress(
            @PathVariable Long orderId,
            @Valid @RequestBody OrderAddressRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        orderFacade.updateAddress(orderId, request, userDetails.getUserId());
        return ApiResponse.success(null);
    }

    // [구매자] 결제 /api/orders/{orderId}/pay
    @Operation(security = @SecurityRequirement(name = "bearerAuth"))
    @PostMapping("/{orderId}/pay")
    public ApiResponse<Void> payOrder(
            @PathVariable Long orderId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        orderFacade.payOrder(orderId, userDetails.getUserId());
        return ApiResponse.success(null);
    }

    // [판매자] 송장 번호 등록 /api/orders/{orderId}/tracking
    @Operation(security = @SecurityRequirement(name = "bearerAuth"))
    @PostMapping("/{orderId}/tracking")
    public ApiResponse<Void> registerTracking(
            @PathVariable Long orderId,
            @Valid @RequestBody OrderTrackingRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        orderFacade.registerTracking(orderId, request, userDetails.getUserId());
        return ApiResponse.success(null);
    }

    // [참여자] 내 거래 내역 조회 /api/orders/me
    @Operation(security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping("/me")
    public ApiResponse<List<OrderResponse>> getMyOrders(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(required = false) OrderHistoryRole role,
            @RequestParam(required = false) OrderFilter filter) {
        List<OrderResponse> myOrders = orderService.getMyOrders(userDetails.getUserId(), role, filter);
        return ApiResponse.success(myOrders);
    }
}
