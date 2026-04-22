package com.team8.backend.domain.order.validator.payment;

import com.team8.backend.domain.order.entity.Order;

public interface PaymentValidationStrategy {
    void validate(Order order);
}
