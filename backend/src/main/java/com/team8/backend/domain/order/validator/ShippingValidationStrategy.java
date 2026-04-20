package com.team8.backend.domain.order.validator;

import com.team8.backend.domain.order.entity.Order;

public interface ShippingValidationStrategy {
    void validate(Order order);
}
