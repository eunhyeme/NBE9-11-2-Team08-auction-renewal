package com.team8.backend.domain.order.service;

import com.team8.backend.domain.order.repository.OrderRepository;
import com.team8.backend.global.exception.CustomException;
import com.team8.backend.global.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class OrderServiceExceptionTest {

    @Autowired
    private OrderService orderService;

    @MockitoBean
    private OrderRepository orderRepository;

    @Test
    @DisplayName("주문 조회 시 ID가 존재하지 않으면 ORDER_NOT_FOUND(400-11) 에러를 던진다")
    void getOrderOrThrow_NotFound() {
        // Given
        Long invalidOrderId = 1L;
        when(orderRepository.findById(invalidOrderId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> orderService.getOrderOrThrow(invalidOrderId))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.ORDER_NOT_FOUND.getMessage());
    }
}
