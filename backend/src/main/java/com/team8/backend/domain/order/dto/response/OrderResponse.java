package com.team8.backend.domain.order.dto.response;

import com.team8.backend.domain.order.entity.Order;
import com.team8.backend.domain.order.entity.OrderStatus;

import java.time.LocalDateTime;

public record OrderResponse(
        Long orderId,
        Long auctionId,
        Long bidderId,
        Long sellerId,
        Long finalPrice,
        OrderStatus status,
        String shippingAddress,
        String trackingNumber,
        LocalDateTime createdAt,
        LocalDateTime paidAt,
        LocalDateTime paymentDeadline,
        LocalDateTime deliveryDeadline,
        Long version
) {
    public static OrderResponse from(Order order) {
        return new OrderResponse(
                order.getOrderId(),
                order.getAuction().getAuctionId(),
                order.getBidder().getUserId(),
                order.getSeller().getUserId(),
                order.getFinalPrice(),
                order.getStatus(),
                order.getShippingAddress(),
                order.getTrackingNumber(),
                order.getCreatedAt(),
                order.getPaidAt(),
                order.getPaymentDeadline(),
                order.getDeliveryDeadline(),
                order.getVersion()
        );
    }
}
