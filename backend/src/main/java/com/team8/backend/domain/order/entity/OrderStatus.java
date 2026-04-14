package com.team8.backend.domain.order.entity;

public enum OrderStatus {
    AWAITING_PAYMENT, //결제 대기
    PAID, // 결제 완료
    DELIVERING, // 배송 중
    DELIVERED, // 배송 완료
    NOT_PAID, // 결제가 안 되어 취소
    NOT_DELIVERED // 발송이 안 되어 취소
}
