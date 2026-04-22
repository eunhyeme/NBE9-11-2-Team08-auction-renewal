package com.team8.backend.domain.order.validator.address;

import com.team8.backend.domain.order.entity.Order;
import com.team8.backend.global.exception.CustomException;
import com.team8.backend.global.exception.ErrorCode;
import org.springframework.stereotype.Component;

// 1. 결제 대기 (AWAITING_PAYMENT), 결제 완료(PAID)가 아니면 배송지 수정 불가
@Component
@org.springframework.core.annotation.Order(1)
public class AddressStatusStrategy implements AddressValidationStrategy {
    @Override
    public void validate(Order order) {
        if (!order.isAddressUpdateEligible()) {
            throw new CustomException(ErrorCode.SHIPPING_ALREADY_STARTED);
        }
    }

    
}
