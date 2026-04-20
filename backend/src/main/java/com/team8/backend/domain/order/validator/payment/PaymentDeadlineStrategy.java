package com.team8.backend.domain.order.validator.payment;

import com.team8.backend.domain.order.entity.Order;
import com.team8.backend.domain.order.validator.PaymentValidationStrategy;
import com.team8.backend.global.exception.CustomException;
import com.team8.backend.global.exception.ErrorCode;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

// 3. 결제 기한 (48시간)이 지나지 않았어야 함
@Component
@org.springframework.core.annotation.Order(3)
public class PaymentDeadlineStrategy implements PaymentValidationStrategy {
    @Override
    public void validate(Order order) {
        if (LocalDateTime.now().isAfter(order.getPaymentDeadline())) {
            throw new CustomException(ErrorCode.PAYMENT_EXPIRED); // 400-7
        }
    }
}
