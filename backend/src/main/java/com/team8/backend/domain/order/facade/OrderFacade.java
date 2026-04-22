package com.team8.backend.domain.order.facade;

import com.team8.backend.domain.order.dto.request.OrderAddressRequest;
import com.team8.backend.domain.order.dto.request.OrderTrackingRequest;
import com.team8.backend.domain.order.dto.response.OrderResponse;
import com.team8.backend.domain.order.entity.Order;
import com.team8.backend.domain.order.service.OrderService;
import com.team8.backend.domain.order.validator.OrderAddressValidator;
import com.team8.backend.domain.order.validator.OrderPaymentValidator;
import com.team8.backend.domain.order.validator.OrderShippingValidator;
import com.team8.backend.domain.order.validator.authority.OrderParticipantAuthorityValidator;
import com.team8.backend.domain.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class OrderFacade {

    private final OrderService orderService;
    private final UserService userService;
    private final OrderAddressValidator orderAddressValidator;
    private final OrderPaymentValidator paymentValidator;
    private final OrderShippingValidator shippingValidator;
    private final OrderParticipantAuthorityValidator orderParticipantAuthorityValidator;
    
    
    // [참여자] 주문 상세 조회
    public OrderResponse getOrder(Long orderId, Long userId) {
        Order order = orderService.getOrderOrThrow(orderId);
        orderParticipantAuthorityValidator.validate(order,userId);
        return OrderResponse.from(order);
    }
    
    // [구매자] 배송 정보 입력
    @Transactional
    public void updateAddress(Long orderId, OrderAddressRequest request, Long userId) {
        // 낙관성 락을 적용한 조회 로직
        Order order = orderService.getOrderWithLockOrThrow(orderId);

        // [구매자] 배송지 수정 가능 여부
        orderAddressValidator.validate(order,userId);

        // [구매자] 배송지 수정 
        orderService.updateAddress(order, request.shippingAddress());

        // 주문 과정에서 기본 배송지로 저장 시 업데이트 
        if (request.saveAsDefault()) {
            userService.updateDefaultAddress(userId, request.shippingAddress());
        }
    }
    
    // [구매자] 결제 처리 (AWAITING_PAYMENT -> PAID) 
    @Transactional
    public void payOrder(Long orderId, Long userId) {
        // 낙관성 락을 적용한 조회 로직
        Order order = orderService.getOrderWithLockOrThrow(orderId);

        // [구매자] 결제 가능 여부 
        paymentValidator.validate(order, userId);

        // [구매자] 결제 처리 (AWAITING_PAYMENT -> PAID) 
        orderService.processPayment(order);
    }

    // [판매자] 송장 번호 등록 및 배송 시작  (PAID-> DELIVERING)
    @Transactional
    public void registerTracking(Long orderId, OrderTrackingRequest request, Long userId) {
        // 낙관성 락을 적용한 조회 로직
        Order order = orderService.getOrderWithLockOrThrow(orderId);

        // [판매자] 송장 등록 가능 여부
        shippingValidator.validate(order, userId);

        // [판매자] 송장 번호 등록 및 배송 시작  (PAID-> DELIVERING)
        orderService.processShipping(order, request.trackingNumber());
    }
}
