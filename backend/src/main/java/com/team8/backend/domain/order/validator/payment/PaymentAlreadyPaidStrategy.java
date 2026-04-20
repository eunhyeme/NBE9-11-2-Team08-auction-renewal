package com.team8.backend.domain.order.validator.payment;

import com.team8.backend.domain.order.entity.Order;
import com.team8.backend.domain.order.validator.PaymentValidationStrategy;
import com.team8.backend.global.exception.CustomException;
import com.team8.backend.global.exception.ErrorCode;
import org.springframework.stereotype.Component;

// 1. 결제가 완료된 상태에서 결제 시 400-8 에러코드
@Component
@org.springframework.core.annotation.Order(1)
public class PaymentAlreadyPaidStrategy implements PaymentValidationStrategy {

    @Override
    public void validate(Order order) {
        if (order.isAlreadyPaid()) {
            throw new CustomException(ErrorCode.ALREADY_PAID);
        }
    }
}
