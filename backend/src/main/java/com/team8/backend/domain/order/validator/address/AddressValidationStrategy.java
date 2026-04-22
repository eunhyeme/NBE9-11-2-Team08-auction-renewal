package com.team8.backend.domain.order.validator.address;

import com.team8.backend.domain.order.entity.Order;

public interface AddressValidationStrategy {
    void validate(Order order);
}
