package com.team8.backend.domain.order.validator;

import com.team8.backend.domain.order.entity.Order;
import com.team8.backend.domain.order.validator.address.AddressValidationStrategy;
import com.team8.backend.domain.order.validator.authority.BuyerAuthorityValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

// [구매자] 배송지 수정 가능 여부
@Component
@RequiredArgsConstructor
public class OrderAddressValidator {
    private final List<AddressValidationStrategy> strategies;
    private final BuyerAuthorityValidator buyerAuthorityValidator;
    
    public void validate(Order order, Long userId) {
        buyerAuthorityValidator.validate(order,userId);
        strategies.forEach(strategy -> strategy.validate(order));
    }
}
