package com.team8.backend.domain.order.scheduler;

import com.team8.backend.domain.order.entity.Order;
import com.team8.backend.domain.order.entity.OrderStatus;
import com.team8.backend.domain.order.repository.OrderRepository;
import com.team8.backend.domain.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.OptimisticLockingFailureException;
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
            int maxRetries = 3;
            for (int i = 0; i < maxRetries; i++) {
                try {
                    orderService.notPaidOrder(order.getOrderId());
                    break; // 성공 시 루프 탈출
                } catch (OptimisticLockingFailureException e) {
                    log.warn("미결제 처리 낙관적 락 충돌 (재시도 {}/{}): 오더 ID {}", i + 1, maxRetries, order.getOrderId(), e);
                    if (i == maxRetries - 1) {
                        log.error("오더 {} 미결제 처리 최종 실패 (최대 재시도 초과)", order.getOrderId(), e);
                    } else {
                        backoff(i);
                    }
                } catch (Exception e) {
                    log.error("오더 {} 미결제 처리 중 예상치 못한 오류: {}", order.getOrderId(), e.getMessage(), e);
                    break;
                }
            }
        });
    }

    // 미출고 대상 처리 (PAID -> NOT_DELIVERED)
    private void processDeliveryTimeouts(LocalDateTime now) {
        List<Order> targets = orderRepository.findByStatusAndDeliveryDeadlineBefore(
                OrderStatus.PAID, now
        );

        targets.forEach(order -> {
            int maxRetries = 3;
            for (int i = 0; i < maxRetries; i++) {
                try {
                    orderService.notDeliveredOrder(order.getOrderId());
                    break; // 성공 시 루프 탈출
                } catch (OptimisticLockingFailureException e) {
                    log.warn("미출고 처리 낙관적 락 충돌 (재시도 {}/{}): 오더 ID {}", i + 1, maxRetries, order.getOrderId(), e);
                    if (i == maxRetries - 1) {
                        log.error("오더 {} 미출고 처리 최종 실패 (최대 재시도 초과)", order.getOrderId(), e);
                    } else {
                        backoff(i);
                    }
                } catch (Exception e) {
                    log.error("오더 {} 미출고 처리 중 예상치 못한 오류: {}", order.getOrderId(), e.getMessage(), e);
                    break;
                }
            }
        });
    }

    private void backoff(int retryIndex) {
        try {
            // i가 0부터 시작하므로 (100ms, 200ms, 300ms...) 점진적 대기
            Thread.sleep(100L * (retryIndex + 1));
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }
}
