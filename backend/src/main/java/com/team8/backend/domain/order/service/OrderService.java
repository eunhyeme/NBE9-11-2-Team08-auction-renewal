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
import java.util.List;

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

    // [참여자] 주문 상세 조회
    public OrderResponse getOrder(Long orderId, Long userId) {
        Order order = orderRepository.getOrThrow(orderId);
        order.validateParticipant(userId);
        return OrderResponse.from(order);
    }

    // [구매자] 배송지 등록/수정
    @Transactional
    public void updateAddress(Long orderId, OrderAddressRequest request, Long userId) {
        Order order = orderRepository.getOrThrow(orderId);
        order.validateBuyer(userId);
        order.validateAddressUpdateEligible();
        order.updateShippingAddress(request.shippingAddress());
    }

    // [구매자] 결제 처리 (AWAITING_PAYMENT -> PAID)
    @Transactional
    public void payOrder(Long orderId, Long userId) {
        Order order = orderRepository.getOrThrow(orderId);
        order.validateBuyer(userId);
        order.validatePaymentEligible();
        order.pay(LocalDateTime.now());
    }

    // [판매자] 송장 번호 등록 및 배송 시작  (PAID-> DELIVERING)
    @Transactional
    public void registerTracking(Long orderId, OrderTrackingRequest request, Long userId) {
        Order order = orderRepository.getOrThrow(orderId);
        order.validateSeller(userId);
        order.validateShippingEligible();
        order.startDelivery(request.trackingNumber());
    }

    // [스케줄러] 미결제 처리 (AWAITING_PAYMENT -> NOT_PAID)
    @Transactional
    public void notPaidOrder(Long orderId) {
        Order order = orderRepository.getOrThrow(orderId);
        order.markNotPaid();
    }

    // [스케줄러] 미출고 처리 (PAID -> NOT_DELIVERED)
    @Transactional
    public void notDeliveredOrder(Long orderId) {
        Order order = orderRepository.getOrThrow(orderId);
        order.markNotDelivered();
    }

    // [참여자] 내 거래 내역 조회 (구매/판매 통합)
    public List<OrderResponse> getMyOrders(Long userId) {
        // 참여자 주문 목록을 가져오기
        List<Order> orders = orderRepository.findAllByUserId(userId);

        // 조회된 엔티티 리스트를 스트림을 통해 응답용 DTO 리스트로 변환
        return orders.stream()
                .map(OrderResponse::from)
                .toList();
    }
}
