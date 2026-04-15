package com.team8.backend.domain.membership.dto.request;

import com.team8.backend.domain.membership.entity.MembershipStatus;
import jakarta.validation.constraints.NotNull;

public record MembershipReviewRequest(
        @NotNull(message = "처리 상태는 필수입니다.")
        MembershipStatus status,
        String reason
) {
}
