package com.team8.backend.domain.order.service;

import com.team8.backend.domain.auction.entity.Auction;
import com.team8.backend.domain.order.dto.request.OrderAddressRequest;
import com.team8.backend.domain.order.dto.request.OrderTrackingRequest;
import com.team8.backend.domain.order.dto.response.OrderResponse;
import com.team8.backend.domain.order.entity.Order;
import com.team8.backend.domain.order.repository.OrderRepository;
import com.team8.backend.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderService {

    private final OrderRepository orderRepository;

    // 경매 낙찰 시 주문 생성
    @Transactional
    public void createOrder(Auction auction, User bidder, Long finalPrice) {
        orderRepository.save(
                new Order(
                        auction,
                        bidder,
                        auction.getSeller(),
                        finalPrice,
                        LocalDateTime.now()));
    }

    // 주문 상세 조회
    public OrderResponse getOrder(Long orderId) {
        return OrderResponse.from(getOrThrow(orderId));
    }

    // [구매자] 배송지 정보 업데이트
    @Transactional
    public void updateAddress(Long orderId, OrderAddressRequest request) {
        Order order = getOrThrow(orderId);
        order.validateAddressUpdateEligible();
        order.updateShippingAddress(request.shippingAddress());
    }

    // [판매자] 송장 번호 등록 및 배송 시작  (PAID-> DELIVERING)
    @Transactional
    public void registerTracking(Long orderId, OrderTrackingRequest request) {
        Order order = getOrThrow(orderId);
        order.validateShippingEligible();
        order.startDelivery(request.trackingNumber());
    }

    // [구매자] 결제 처리 (AWAITING_PAYMENT -> PAID)
    @Transactional
    public void payOrder(Long orderId) {
        Order order = getOrThrow(orderId);
        order.validatePaymentEligible();
        order.pay(LocalDateTime.now());
    }

    // [스케줄러] 미결제 처리 (AWAITING_PAYMENT -> NOT_PAID)
    @Transactional
    public void notPaidOrder(Long orderId) {
        Order order = getOrThrow(orderId);
        order.markNotPaid();
    }

    // [스케줄러[ 미출고 처리 (PAID -> NOT_DELIVERED)
    @Transactional
    public void notDeliveredOrder(Long orderId) {
        Order order = getOrThrow(orderId);
        order.markNotDelivered();
    }

    // 공통 조회 로직
    private Order getOrThrow(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("주문을 찾을 수 없습니다. ID: " + orderId));
    }
}
