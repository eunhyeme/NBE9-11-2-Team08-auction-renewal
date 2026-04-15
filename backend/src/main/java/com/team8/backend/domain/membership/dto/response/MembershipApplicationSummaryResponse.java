package com.team8.backend.domain.membership.dto.response;

import com.team8.backend.domain.membership.entity.MembershipApplication;
import com.team8.backend.domain.membership.entity.MembershipStatus;

import java.time.LocalDateTime;


public record MembershipApplicationSummaryResponse(
        Long applicationId,
        Long userId,
        String email,
        String nickname,
        MembershipStatus status,
        String reason,
        LocalDateTime requestedAt,
        LocalDateTime reviewedAt,
        Long reviewedBy
) {
    public static MembershipApplicationSummaryResponse from(MembershipApplication application) {
        return new MembershipApplicationSummaryResponse(
                application.getId(),
                application.getUser().getUserId(),
                application.getUser().getEmail(),
                application.getUser().getNickname(),
                application.getStatus(),
                application.getReason(),
                application.getRequestedAt(),
                application.getReviewedAt(),
                application.getReviewedBy()
        );
    }
}
