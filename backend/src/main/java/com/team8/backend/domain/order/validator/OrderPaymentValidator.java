package com.team8.backend.domain.order.validator;

import com.team8.backend.domain.order.entity.Order;
import com.team8.backend.domain.order.validator.authority.BuyerAuthorityValidator;
import com.team8.backend.domain.order.validator.payment.PaymentValidationStrategy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

// [구매자] 결제 가능 여부 
@Component
@RequiredArgsConstructor
public class OrderPaymentValidator {
    private final List<PaymentValidationStrategy> strategies;
    private final BuyerAuthorityValidator buyerAuthorityValidator;
    
    public void validate(Order order, Long userId) {
        buyerAuthorityValidator.validate(order, userId);
        strategies.forEach(strategy -> strategy.validate(order)); // 세부 전략 실행
        
    }
}
