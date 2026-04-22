package com.team8.backend.domain.order.validator.shipping;

import com.team8.backend.domain.order.entity.Order;
import com.team8.backend.global.exception.CustomException;
import com.team8.backend.global.exception.ErrorCode;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

// 3. 발송 기한 (72시간) 내에 등록해야 함
@Component
@org.springframework.core.annotation.Order(3)
public class ShippingDeadlineStrategy implements ShippingValidationStrategy {
    public void validate(Order order) {
        if (LocalDateTime.now().isAfter(order.getDeliveryDeadline())) {
            throw new CustomException(ErrorCode.DELIVERY_EXPIRED); // 400-18
        }
    }
}
