package com.team8.backend.domain.order.validator;

import com.team8.backend.domain.order.entity.Order;
import com.team8.backend.domain.order.validator.authority.SellerAuthorityValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

//  [판매자] 송장 등록 가능 여부
@Component
@RequiredArgsConstructor
public class OrderShippingValidator {
    private final List<ShippingValidationStrategy> strategies;
    private final SellerAuthorityValidator sellerAuthorityValidator;

    public void validate(Order order, Long userId) {
        sellerAuthorityValidator.validate(order, userId);
        strategies.forEach(strategy -> strategy.validate(order)); // 세부 전략 실행
    }
}
