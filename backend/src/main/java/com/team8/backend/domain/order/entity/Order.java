package com.team8.backend.domain.order.entity;

import com.team8.backend.domain.auction.entity.Auction;
import com.team8.backend.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "orders")
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

    protected Order() {
    }

    public Order(Auction auction, User bidder, User seller, Long finalPrice, LocalDateTime createdAt) {
        this.auction = auction;
        this.bidder = bidder;
        this.seller = seller;
        this.finalPrice = finalPrice;
        this.status = OrderStatus.AWAITING_PAYMENT;
        this.paymentDeadline = createdAt.plusHours(48);
        this.createdAt = createdAt;
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
        this.status = OrderStatus.NOT_PAID;
    }

    public void markNotDelivered() {
        this.status = OrderStatus.NOT_DELIVERED;
    }

    public void updateShippingAddress(String shippingAddress) {
        this.shippingAddress = shippingAddress;
    }

    // 배송지 수정 가능 여부
    public void validateAddressUpdateEligible() {
        if (this.status != OrderStatus.PAID) {
            throw new RuntimeException("결제 완료 상태에서만 배송지 입력 및 수정이 가능합니다.");
        }
    }

    // 송장 등록 가능 여부
    public void validateShippingEligible() {
        if (this.status != OrderStatus.PAID) {
            throw new RuntimeException("결제 완료된 주문만 송장을 등록할 수 있습니다.");
        }
        if (this.shippingAddress == null || this.shippingAddress.trim().isEmpty()) {
            throw new RuntimeException("배송지 정보가 입력되지 않아 송장을 등록할 수 없습니다.");
        }
        if (this.trackingNumber != null) {
            throw new RuntimeException("이미 송장이 등록되어 있습니다.");
        }
        if (LocalDateTime.now().isAfter(this.deliveryDeadline)) {
            this.markNotDelivered();
            throw new RuntimeException("발송 기한이 만료되어 미출고 처리되었습니다.");
        }
    }

    // 결제 가능 여부
    public void validatePaymentEligible() {
        if (this.status != OrderStatus.AWAITING_PAYMENT) {
            throw new RuntimeException("이미 결제 완료된 주문입니다.");
        }
        if (LocalDateTime.now().isAfter(this.paymentDeadline)) {
            throw new RuntimeException("결제 시간이 만료되었습니다.");
        }
    }
}
