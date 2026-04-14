package com.team8.backend.domain.order.repository;

import com.team8.backend.domain.order.entity.Order;
import com.team8.backend.domain.order.entity.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {
    // 결제 기한 체크
    List<Order> findByStatusAndPaymentDeadlineBefore(OrderStatus status, LocalDateTime now);

    // 발송 기한 체크
    List<Order> findByStatusAndDeliveryDeadlineBefore(OrderStatus status, LocalDateTime now);

}
