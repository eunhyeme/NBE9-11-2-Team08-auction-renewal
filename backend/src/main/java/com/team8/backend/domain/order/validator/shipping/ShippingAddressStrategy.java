package com.team8.backend.domain.order.validator.shipping;

import com.team8.backend.domain.order.entity.Order;
import com.team8.backend.global.exception.CustomException;
import com.team8.backend.global.exception.ErrorCode;
import org.springframework.stereotype.Component;

// 4. 구매자가 배송지를 입력했어야 함
@Component
@org.springframework.core.annotation.Order(4)
public class ShippingAddressStrategy implements ShippingValidationStrategy {
    @Override
    public void validate(Order order) {
        if (order.getShippingAddress() == null || order.getShippingAddress().isBlank()) {
            throw new CustomException(ErrorCode.SHIPPING_ADDRESS_MISSING); // 400-17
        }
    }
}
