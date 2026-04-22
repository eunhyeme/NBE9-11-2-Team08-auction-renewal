package com.team8.backend.domain.order.validator.shipping;

import com.team8.backend.domain.order.entity.Order;
import com.team8.backend.global.exception.CustomException;
import com.team8.backend.global.exception.ErrorCode;
import org.springframework.stereotype.Component;

// 2. 이미 송장이 등록되어있으면 안 됨
@Component
@org.springframework.core.annotation.Order(2)
public class ShippingDuplicateStrategy implements ShippingValidationStrategy {
    public void validate(Order order) {
        if (order.getTrackingNumber() != null) {
            throw new CustomException(ErrorCode.TRACKING_ALREADY_EXISTS);  // 400-9
        }
    }
}
