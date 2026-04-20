package com.team8.backend.domain.order.entity;

import com.team8.backend.domain.auction.entity.Auction;
import com.team8.backend.domain.user.entity.User;
import com.team8.backend.global.exception.CustomException;
import com.team8.backend.global.exception.ErrorCode;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.EnumSet;

@Getter
@Entity
@Table(name = "orders")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_id")
    private Long orderId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "auction_id", nullable = false, unique = true)
    private Auction auction;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bidder_id", nullable = false)
    private User bidder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id", nullable = false)
    private User seller;

    @Column(name = "final_price", nullable = false)
    private Long finalPrice;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;

    @Column(name = "payment_deadline")
    private LocalDateTime paymentDeadline;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Column(name = "shipping_address")
    private String shippingAddress;

    @Column(name = "tracking_number")
    private String trackingNumber;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    // 발송 기한
    @Column(name = "delivery_deadline")
    private LocalDateTime deliveryDeadline;

    @Version // 낙관적 락을 위한 필드 추가
    @Column(name = "version", nullable = false)
    private Long version;
    
    
    @Builder
    public Order(Auction auction, User bidder, User seller, Long finalPrice, String shippingAddress, LocalDateTime createdAt) {
        this.auction = auction;
        this.bidder = bidder;
        this.seller = seller;
        this.finalPrice = finalPrice;
        this.status = OrderStatus.AWAITING_PAYMENT;
        this.paymentDeadline = createdAt.plusHours(48);
        this.createdAt = createdAt;
        this.shippingAddress = shippingAddress;
        this.version = 0L;
    }

    public void pay(LocalDateTime paidAt) {
        this.status = OrderStatus.PAID;
        this.paidAt = paidAt;
        this.deliveryDeadline = paidAt.plusHours(72);
    }

    public void startDelivery(String trackingNumber) {
        this.status = OrderStatus.DELIVERING;
        this.trackingNumber = trackingNumber;
    }

    public void completeDelivery() {
        this.status = OrderStatus.DELIVERED;
    }

    public void markNotPaid() {
        // 시간 체크
        if (this.status != OrderStatus.AWAITING_PAYMENT ||
                this.paymentDeadline.isAfter(LocalDateTime.now())) {
            return;
        }
        this.status = OrderStatus.NOT_PAID;
    }

    public void markNotDelivered() {
        // 시간 체크
        if (this.status != OrderStatus.PAID || this.deliveryDeadline.isAfter(LocalDateTime.now())) {
            return;
        }
        this.status = OrderStatus.NOT_DELIVERED;
    }

    public void updateShippingAddress(String shippingAddress) {
        this.shippingAddress = shippingAddress;
    }

    // [구매자] 배송지 수정 가능 여부
    public void validateAddressUpdateEligible() {
        // 결제 대기 (AWAITING_PAYMENT), 결제 완료(PAID)가 아니면 배송지 수정 불가
        if (this.status != OrderStatus.AWAITING_PAYMENT && this.status != OrderStatus.PAID) {
            throw new CustomException(ErrorCode.SHIPPING_ALREADY_STARTED); // 400-19
        }
    }
    
    // [구매자] 권한 체크
    public boolean isBuyer(Long userId) {
        return this.bidder.getUserId().equals(userId);
    }

    // [판매자] 권한 체크
    public boolean isSeller(Long userId) {
        return this.seller.getUserId().equals(userId);
    }

    // [참여자] 권한 체크
    public boolean isParticipant(Long userId) {
        return isBuyer(userId) || isSeller(userId);
    }
    
    // [구매자] 결제가 완료된 상태인지
    public boolean isAlreadyPaid() {
        return EnumSet.of(
                OrderStatus.PAID,
                OrderStatus.DELIVERING,
                OrderStatus.DELIVERED,
                OrderStatus.NOT_DELIVERED
        ).contains(this.status);
    }
}
