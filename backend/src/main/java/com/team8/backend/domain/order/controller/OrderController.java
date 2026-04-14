package com.team8.backend.domain.order.controller;

import com.team8.backend.domain.order.dto.request.OrderAddressRequest;
import com.team8.backend.domain.order.dto.request.OrderTrackingRequest;
import com.team8.backend.domain.order.dto.response.OrderResponse;
import com.team8.backend.domain.order.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    // 주문 상세 조회 /api/orders/{orderId}
    @GetMapping("/{orderId}")
    public OrderResponse getOrder(@PathVariable Long orderId) {
        return orderService.getOrder(orderId);
    }

    // 배송 정보 입력 (구매자용) /api/orders/{orderId}/shipping
    @PostMapping("/{orderId}/shipping")
    public void inputShipping(
            @PathVariable Long orderId,
            @Valid @RequestBody OrderAddressRequest request) {
        orderService.updateAddress(orderId, request);
    }

    // 송장 등록 (판매자용) /api/orders/{orderId}/tracking
    @PostMapping("/{orderId}/tracking")
    public void registerTracking(
            @PathVariable Long orderId,
            @Valid @RequestBody OrderTrackingRequest request) {
        orderService.registerTracking(orderId, request);
    }

    // 결제 (구매자용) /api/orders/{orderId}/pay
    @PostMapping("/{orderId}/pay")
    public void payOrder(@PathVariable Long orderId) {
        orderService.payOrder(orderId);
    }


}