package com.team8.backend.domain.order.entity;

import com.team8.backend.domain.auction.entity.Auction;
import com.team8.backend.domain.user.entity.User;
import com.team8.backend.global.exception.CustomException;
import com.team8.backend.global.exception.ErrorCode;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

@SuperBuilder
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
    @Builder.Default // 💡 빌더를 통해 생성할 때도 0L이 들어가도록 보장합니다.
    @Column(name = "version", nullable = false)
    private Long version = 0L;

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

    // [구매자] 결제 가능 여부
    public void validatePaymentEligible() {
        // 결제 대기 (AWAITING_PAYMENT)에서만 가능
        if (this.status != OrderStatus.AWAITING_PAYMENT) {
            throw new CustomException(ErrorCode.ALREADY_PAID); // 400-8
        }
        // 결제 기한 (48시간)이 지나지 않았어야 함
        if (LocalDateTime.now().isAfter(this.paymentDeadline)) {
            throw new CustomException(ErrorCode.PAYMENT_EXPIRED); // 400-7
        }
        // 결제 전 반드시 배송지가 입력돼 있어야 함
        if (this.shippingAddress == null || this.shippingAddress.isBlank()) {
            throw new CustomException(ErrorCode.SHIPPING_ADDRESS_MISSING); // 400-17
        }
    }

    //  [판매자] 송장 등록 가능 여부
    public void validateShippingEligible() {
        // 결제가 완료(PAID) 상태여야 함
        if (this.status != OrderStatus.PAID) {
            throw new CustomException(ErrorCode.INVALID_ORDER_STATE); // 400-6
        }
        // 구매자가 배송지를 입력했어야 함
        if (this.shippingAddress == null || this.shippingAddress.isBlank()) {
            throw new CustomException(ErrorCode.SHIPPING_ADDRESS_MISSING); // 400-17
        }
        // 이미 송장이 등록되어있으면 안 됨
        if (this.trackingNumber != null) {
            throw new CustomException(ErrorCode.TRACKING_ALREADY_EXISTS); // 400-9
        }
        // 발송 기한 (72시간) 내에 등록해야 함
        if (LocalDateTime.now().isAfter(this.deliveryDeadline)) {
            throw new CustomException(ErrorCode.DELIVERY_EXPIRED); // 400-18
        }
    }

    // [구매자] 권한 체크
    public void validateBuyer(Long userId) {
        if (!this.bidder.getUserId().equals(userId)) {
            throw new CustomException(ErrorCode.ACCESS_DENIED); // 403-1
        }
    }

    // [판매자] 권한 체크
    public void validateSeller(Long userId) {
        if (!this.seller.getUserId().equals(userId)) {
            throw new CustomException(ErrorCode.ACCESS_DENIED); // 403-1
        }
    }

    // [참여자] 권한 체크
    public void validateParticipant(Long userId) {
        if (!this.bidder.getUserId().equals(userId) && !this.seller.getUserId().equals(userId)) {
            throw new CustomException(ErrorCode.ACCESS_DENIED); // 403-1
        }
    }
}
