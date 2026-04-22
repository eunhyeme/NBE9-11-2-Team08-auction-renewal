package com.team8.backend.domain.order.scheduler;

import com.team8.backend.domain.order.entity.Order;
import com.team8.backend.domain.order.entity.OrderStatus;
import com.team8.backend.domain.order.repository.OrderRepository;
import com.team8.backend.domain.order.service.OrderService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.OptimisticLockingFailureException;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderSchedulerRetryTest {

    @InjectMocks private OrderScheduler orderScheduler;
    @Mock private OrderRepository orderRepository;
    @Mock private OrderService orderService;

    @Test
    @DisplayName("낙관적 락 충돌 시 최대 3번까지 재시도한다")
    void retryOnOptimisticLockingFailure() {
        // Given: 만료된 주문 1건이 있다고 가정
        Order mockOrder = mock(Order.class);
        given(mockOrder.getOrderId()).willReturn(1L);
        given(orderRepository.findByStatusAndPaymentDeadlineBefore(eq(OrderStatus.AWAITING_PAYMENT), any()))
                .willReturn(List.of(mockOrder));

        // 두 번은 실패하고 세 번째에 성공하도록 설정
        doThrow(new OptimisticLockingFailureException("충돌"))
                .doThrow(new OptimisticLockingFailureException("충돌"))
                .doNothing()
                .when(orderService).notPaidOrder(1L);

        // When
        orderScheduler.handleOrderTimeouts();

        // Then: 서비스 메서드가 총 3번 호출되었는지 확인
        verify(orderService, times(3)).notPaidOrder(1L);
    }
}
