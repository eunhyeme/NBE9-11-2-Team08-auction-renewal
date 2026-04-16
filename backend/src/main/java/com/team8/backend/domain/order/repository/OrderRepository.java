package com.team8.backend.domain.order.repository;

import com.team8.backend.domain.order.entity.Order;
import com.team8.backend.domain.order.entity.OrderStatus;
import com.team8.backend.global.exception.CustomException;
import com.team8.backend.global.exception.ErrorCode;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {
    // 공통 조회 로직
    default Order getOrThrow(Long orderId) {
        return findById(orderId)
                .orElseThrow(() -> new CustomException(ErrorCode.ORDER_NOT_FOUND)); // 404-3
    }
    // 낙관성 락을 적용한 조회 로직
    default Order getWithLockOrThrow(Long orderId) {
        return findByIdWithLock(orderId)
                .orElseThrow(() -> new CustomException(ErrorCode.ORDER_NOT_FOUND)); // 404-3
    }

    // 결제 기한 만료 주문 조회
    List<Order> findByStatusAndPaymentDeadlineBefore(OrderStatus status, LocalDateTime now);

    // 발송 기한 만료 주문 조회
    List<Order> findByStatusAndDeliveryDeadlineBefore(OrderStatus status, LocalDateTime now);

    // 사용자 거래 내역 조회 (구매/판매 통합)
    List<Order> findAllByBidder_UserIdOrderByCreatedAtDesc(Long userId);

    List<Order> findAllByBidder_UserIdAndStatusInOrderByCreatedAtDesc(Long userId, Collection<OrderStatus> statuses);

    List<Order> findAllBySeller_UserIdOrderByCreatedAtDesc(Long userId);

    List<Order> findAllBySeller_UserIdAndStatusInOrderByCreatedAtDesc(Long userId, Collection<OrderStatus> statuses);

    @Query("SELECT o FROM Order o " +
            "WHERE o.bidder.userId = :userId OR o.seller.userId = :userId " +
            "ORDER BY o.createdAt DESC")
    List<Order> findAllByUserId(@Param("userId") Long userId);

    @Query("SELECT o FROM Order o " +
            "WHERE (o.bidder.userId = :userId OR o.seller.userId = :userId) " +
            "AND o.status IN :statuses " +
            "ORDER BY o.createdAt DESC")
    List<Order> findAllByUserIdAndStatusIn(
            @Param("userId") Long userId,
            @Param("statuses") Collection<OrderStatus> statuses
    );
    // 낙관적 락을 적용한 findById
    @Lock(LockModeType.OPTIMISTIC)
    @Query("select o from Order o where o.orderId = :id")
    Optional<Order> findByIdWithLock(@Param("id") Long id);
}
