package com.team8.backend.domain.order.dto.request;

import jakarta.validation.constraints.NotBlank;

public record OrderTrackingRequest(
        @NotBlank(message = "송장 번호는 필수입니다.")
        String trackingNumber
) {
}
