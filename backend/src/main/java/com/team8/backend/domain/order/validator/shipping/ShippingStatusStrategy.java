package com.team8.backend.domain.order.validator.shipping;

import com.team8.backend.domain.order.entity.Order;
import com.team8.backend.domain.order.entity.OrderStatus;
import com.team8.backend.global.exception.CustomException;
import com.team8.backend.global.exception.ErrorCode;
import org.springframework.stereotype.Component;

// 1. 결제가 완료(PAID) 상태여야 함
@Component
@org.springframework.core.annotation.Order(1)
public class ShippingStatusStrategy implements ShippingValidationStrategy {
    public void validate(Order order) {
        if (order.getStatus() != OrderStatus.PAID) {
            throw new CustomException(ErrorCode.INVALID_ORDER_STATE); // 400-6
        }
    }
}
