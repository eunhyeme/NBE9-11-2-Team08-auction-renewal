package com.team8.backend.domain.order.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class OrderEntityTest {

    @Test
    @DisplayName("결제 시 상태가 PAID로 변경되고 72시간의 출고 기한이 설정된다")
    void pay_Success() {
        // Order 빌더는 기본적으로 AWAITING_PAYMENT 상태로 생성함
        Order order = Order.builder().createdAt(LocalDateTime.now()).build();
        LocalDateTime paidAt = LocalDateTime.now();

        order.pay(paidAt);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.PAID);
        assertThat(order.getPaidAt()).isEqualTo(paidAt);
        assertThat(order.getDeliveryDeadline()).isEqualTo(paidAt.plusHours(72));
    }

    @Test
    @DisplayName("48시간 기한이 지난 후에만 markNotPaid가 상태를 변경한다")
    void markNotPaid_TimeCheck() {
        // 49시간 전 생성된 주문
        Order oldOrder = Order.builder().createdAt(LocalDateTime.now().minusHours(49)).build();
        oldOrder.markNotPaid();
        assertThat(oldOrder.getStatus()).isEqualTo(OrderStatus.NOT_PAID);

        // 방금 생성된 주문
        Order newOrder = Order.builder().createdAt(LocalDateTime.now()).build();
        newOrder.markNotPaid();
        assertThat(newOrder.getStatus()).isEqualTo(OrderStatus.AWAITING_PAYMENT); // 변경 안됨
    }
}
