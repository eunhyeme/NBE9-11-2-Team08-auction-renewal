package com.team8.backend.domain.order.entity;

import com.team8.backend.domain.auction.entity.Auction;
import com.team8.backend.domain.user.entity.User;
import jakarta.persistence.*;
import java.time.LocalDateTime;

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

    protected Order() {}

    public Order(Auction auction, User bidder, User seller, Long finalPrice, LocalDateTime paymentDeadline, LocalDateTime createdAt) {
        this.auction = auction;
        this.bidder = bidder;
        this.seller = seller;
        this.finalPrice = finalPrice;
        this.status = OrderStatus.AWAITING_PAYMENT;
        this.paymentDeadline = paymentDeadline;
        this.createdAt = createdAt;
    }

    public Long getOrderId() { return orderId; }
    public Auction getAuction() { return auction; }
    public User getBidder() { return bidder; }
    public User getSeller() { return seller; }
    public Long getFinalPrice() { return finalPrice; }
    public OrderStatus getStatus() { return status; }
    public LocalDateTime getPaymentDeadline() { return paymentDeadline; }
    public LocalDateTime getPaidAt() { return paidAt; }
    public String getShippingAddress() { return shippingAddress; }
    public String getTrackingNumber() { return trackingNumber; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    public void pay(LocalDateTime paidAt) {
        this.status = OrderStatus.PAID;
        this.paidAt = paidAt;
    }

    public void startDelivery(String trackingNumber) {
        this.status = OrderStatus.DELIVERING;
        this.trackingNumber = trackingNumber;
    }

    public void completeDelivery() { this.status = OrderStatus.DELIVERED; }
    public void markNotPaid() { this.status = OrderStatus.NOT_PAID; }
    public void markNotDelivered() { this.status = OrderStatus.NOT_DELIVERED; }

    public void updateShippingAddress(String shippingAddress) {
        this.shippingAddress = shippingAddress;
    }
}
