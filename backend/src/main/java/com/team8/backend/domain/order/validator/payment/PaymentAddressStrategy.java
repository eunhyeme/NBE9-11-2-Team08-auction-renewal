package com.team8.backend.domain.order.validator.payment;

import com.team8.backend.domain.order.entity.Order;
import com.team8.backend.global.exception.CustomException;
import com.team8.backend.global.exception.ErrorCode;
import org.springframework.stereotype.Component;

// 4. 결제 전 반드시 배송지가 입력돼 있어야 함
@Component
@org.springframework.core.annotation.Order(4)
public class PaymentAddressStrategy implements PaymentValidationStrategy {
    @Override
    public void validate(Order order) {
        String address = order.getShippingAddress();

        if (address == null || address.isBlank()) {
            throw new CustomException(ErrorCode.SHIPPING_ADDRESS_MISSING); // 400-17
        }
    }
}
