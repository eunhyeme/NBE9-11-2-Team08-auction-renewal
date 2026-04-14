package com.team8.backend.domain.order.scheduler;

import com.team8.backend.domain.order.entity.Order;
import com.team8.backend.domain.order.entity.OrderStatus;
import com.team8.backend.domain.order.repository.OrderRepository;
import com.team8.backend.domain.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderScheduler {

    private final OrderRepository orderRepository;
    private final OrderService orderService;

    // 매 분 0초에 실행 (서울 시간 기준)
    @Scheduled(cron = "0 * * * * *", zone = "Asia/Seoul")
    public void handleOrderTimeouts() {
        LocalDateTime now = LocalDateTime.now();
        processPaymentTimeouts(now);
        processDeliveryTimeouts(now);
    }

    // 미결제 대상 처리 (AWAITING_PAYMENT -> NOT_PAID)
    private void processPaymentTimeouts(LocalDateTime now) {
        List<Order> targets = orderRepository.findByStatusAndPaymentDeadlineBefore(
                OrderStatus.AWAITING_PAYMENT, now
        );

        targets.forEach(order -> {
            try {
                orderService.notPaidOrder(order.getOrderId());
            } catch (Exception e) {
                log.error("미결제 대상 처리 중 오류 {}: {}", order.getOrderId(), e.getMessage(), e);

            }
        });
    }

    // 미출고 대상 처리 (PAID -> NOT_DELIVERED)
    private void processDeliveryTimeouts(LocalDateTime now) {
        List<Order> targets = orderRepository.findByStatusAndDeliveryDeadlineBefore(
                OrderStatus.PAID, now
        );

        targets.forEach(order -> {
            try {
                orderService.notDeliveredOrder(order.getOrderId());
            } catch (Exception e) {
                log.error("미출고 대상 처리 중 오류 {}: {}", order.getOrderId(), e.getMessage(), e);

            }
        });
    }
}
