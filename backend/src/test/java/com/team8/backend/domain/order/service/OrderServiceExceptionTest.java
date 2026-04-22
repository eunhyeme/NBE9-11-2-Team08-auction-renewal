package com.team8.backend.domain.order.service;

import com.team8.backend.domain.order.repository.OrderRepository;
import com.team8.backend.global.exception.CustomException;
import com.team8.backend.global.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean; // MockitoBean으로 변경

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
public class OrderServiceExceptionTest {

    @Autowired private OrderService orderService;

    @MockitoBean 
    private OrderRepository orderRepository;

    @Test
    @DisplayName("주문 조회 시 ID가 존재하지 않으면 ORDER_NOT_FOUND(400-11) 에러를 던진다")
    void getOrderOrThrow_NotFound() {
        when(orderRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.getOrderOrThrow(1L))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.ORDER_NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("락 기반 주문 조회 시 ID가 존재하지 않으면 ORDER_NOT_FOUND(400-11) 에러를 던진다")
    void getOrderWithLockOrThrow_NotFound() {
        when(orderRepository.findByIdWithLock(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.getOrderWithLockOrThrow(1L))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.ORDER_NOT_FOUND.getMessage());
    }
}
