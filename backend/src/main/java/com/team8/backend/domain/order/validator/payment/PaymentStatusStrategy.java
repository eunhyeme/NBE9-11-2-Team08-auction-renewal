package com.team8.backend.domain.order.validator.payment;

import com.team8.backend.domain.order.entity.Order;
import com.team8.backend.domain.order.entity.OrderStatus;
import com.team8.backend.global.exception.CustomException;
import com.team8.backend.global.exception.ErrorCode;
import org.springframework.stereotype.Component;

// 2. 결제 대기 (AWAITING_PAYMENT)에서만 가능
@Component
@org.springframework.core.annotation.Order(2)
public class PaymentStatusStrategy implements PaymentValidationStrategy {
    @Override
    public void validate(Order order) {
        if (order.getStatus() != OrderStatus.AWAITING_PAYMENT) {
            throw new CustomException(ErrorCode.INVALID_ORDER_STATE); // 400-6
        }
    }
}
