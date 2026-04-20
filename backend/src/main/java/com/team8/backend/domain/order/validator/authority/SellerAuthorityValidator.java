package com.team8.backend.domain.order.validator.authority;

import com.team8.backend.domain.order.entity.Order;
import com.team8.backend.global.exception.CustomException;
import com.team8.backend.global.exception.ErrorCode;
import org.springframework.stereotype.Component;

// [판매자] 권한 체크
@Component
public class SellerAuthorityValidator{
    public void validate(Order order, Long userId) {
        if (!order.isSeller(userId)) {
            throw new CustomException(ErrorCode.ACCESS_DENIED); // 403-1
        }
    }
}
