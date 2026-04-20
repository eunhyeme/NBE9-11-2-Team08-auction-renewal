package com.team8.backend.domain.order.service;

import com.team8.backend.domain.auction.entity.Auction;
import com.team8.backend.domain.order.dto.response.OrderResponse;
import com.team8.backend.domain.order.entity.Order;
import com.team8.backend.domain.order.entity.OrderFilter;
import com.team8.backend.domain.order.entity.OrderHistoryRole;
import com.team8.backend.domain.order.entity.OrderStatus;
import com.team8.backend.domain.order.repository.OrderRepository;
import com.team8.backend.domain.user.entity.User;
import com.team8.backend.global.exception.CustomException;
import com.team8.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderService {
    private final OrderRepository orderRepository;
    
    private static final Set<OrderStatus> ONGOING_STATUSES =
            EnumSet.of(OrderStatus.AWAITING_PAYMENT, OrderStatus.PAID, OrderStatus.DELIVERING);
    private static final Set<OrderStatus> COMPLETED_STATUSES =
            EnumSet.of(OrderStatus.DELIVERED, OrderStatus.NOT_PAID, OrderStatus.NOT_DELIVERED);

    // 경매 낙찰 시 주문 생성
    @Transactional
    public void createOrder(Auction auction, User bidder, Long finalPrice) {
        orderRepository.save(Order.builder()
                .auction(auction)
                .bidder(bidder)
                .seller(auction.getSeller())
                .finalPrice(finalPrice)
                .shippingAddress(bidder.getAddress())
                .createdAt(LocalDateTime.now())
                .build());
    }
    
    // 공통 조회 로직
    public Order getOrderOrThrow(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new CustomException(ErrorCode.ORDER_NOT_FOUND));
    }
    
    // 낙관성 락을 적용한 조회 로직
    public Order getOrderWithLockOrThrow(Long orderId) {
        return orderRepository.findByIdWithLock(orderId)
                .orElseThrow(() -> new CustomException(ErrorCode.ORDER_NOT_FOUND));
    }

    // [구매자] 결제 처리 (AWAITING_PAYMENT -> PAID) 
    @Transactional
    public void processPayment(Order order) {
        order.pay(LocalDateTime.now());
    }

    // [판매자] 송장 번호 등록 및 배송 시작  (PAID-> DELIVERING)
    @Transactional
    public void processShipping(Order order, String trackingNumber) {
        order.startDelivery(trackingNumber);
    }

    // [스케줄러] 미결제 처리 (AWAITING_PAYMENT -> NOT_PAID)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void notPaidOrder(Long orderId) {
        // 낙관성 락을 적용한 조회
        Order order = getOrderWithLockOrThrow(orderId);
        
        // 이미 상태가 변했다면 즉시 종료
        if (order.getStatus() != OrderStatus.AWAITING_PAYMENT) {
            return;
        }
        order.markNotPaid();
    }

    // [스케줄러] 미출고 처리 (PAID -> NOT_DELIVERED)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void notDeliveredOrder(Long orderId) {
        // 낙관성 락을 적용한 조회
        Order order = getOrderWithLockOrThrow(orderId);
        
        //이미 상태가 변했다면 즉시 종료
        if (order.getStatus() != OrderStatus.PAID) {
            return;
        }
        order.markNotDelivered();
    }

    // [참여자] 내 거래 내역 조회
    public List<OrderResponse> getMyOrders(Long userId, OrderHistoryRole role, OrderFilter filter) {
        List<Order> orders;
        Set<OrderStatus> statuses = getStatuses(filter);

        if (role == OrderHistoryRole.BUYER) {
            orders = (statuses == null)
                    ? orderRepository.findAllByBidder_UserIdOrderByCreatedAtDesc(userId)
                    : orderRepository.findAllByBidder_UserIdAndStatusInOrderByCreatedAtDesc(userId, statuses);
        } else if (role == OrderHistoryRole.SELLER) {
            orders = (statuses == null)
                    ? orderRepository.findAllBySeller_UserIdOrderByCreatedAtDesc(userId)
                    : orderRepository.findAllBySeller_UserIdAndStatusInOrderByCreatedAtDesc(userId, statuses);
        } else {
            orders = (statuses == null)
                    ? orderRepository.findAllByUserId(userId)
                    : orderRepository.findAllByUserIdAndStatusIn(userId, statuses);
        }

        // 조회된 엔티티 리스트를 스트림을 통해 응답용 DTO 리스트로 변환
        return orders.stream()
                .map(OrderResponse::from)
                .toList();
    }

    private Set<OrderStatus> getStatuses(OrderFilter filter) {
        if (filter == null) {
            return null;
        }

        return switch (filter) {
            case ONGOING -> ONGOING_STATUSES;
            case COMPLETED -> COMPLETED_STATUSES;
        };
    }
}
