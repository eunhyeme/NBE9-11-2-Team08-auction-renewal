package com.team8.backend.domain.order.validator.shipping;

import com.team8.backend.domain.order.entity.Order;

public interface ShippingValidationStrategy {
    void validate(Order order);
}
