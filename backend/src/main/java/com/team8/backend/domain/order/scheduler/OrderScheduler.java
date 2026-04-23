package com.team8.backend.domain.order.scheduler;

import com.team8.backend.domain.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderScheduler {

    private final OrderRepository orderRepository;

    // 매 초마다 실행
    @Scheduled(fixedDelay = 1000)
    // 매 분 0초에 실행 (서울 시간 기준)
//    @Scheduled(cron = "0 * * * * *", zone = "Asia/Seoul")
    @Transactional // 벌크 업데이트는 하나의 트랜잭션으로 묶어 실행
    public void handleOrderTimeouts() {
        LocalDateTime now = LocalDateTime.now();

        // 1. 미결제 대상 벌크 처리 (AWAITING_PAYMENT -> NOT_PAID)
        int paymentCount = orderRepository.updatePaymentTimeouts(now);

        // 2. 미출고 대상 벌크 처리 (PAID -> NOT_DELIVERED)
        int deliveryCount = orderRepository.updateDeliveryTimeouts(now);

        // 변경된 건수가 있을 때만 로그 출력
        if (paymentCount > 0 || deliveryCount > 0) {
            log.info("[Order] 타임아웃 처리: 미결제 취소 {}건, 미출고 처리 {}건", paymentCount, deliveryCount);
        }
    }
}
