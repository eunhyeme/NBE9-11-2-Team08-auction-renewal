package com.team8.backend.domain.order.dto.request;

import jakarta.validation.constraints.NotBlank;

public record OrderAddressRequest(
        @NotBlank(message = "배송지는 필수 입력 항목입니다.")
        String shippingAddress,
        boolean saveAsDefault // 기본 배송지 저장
) {
}
