package com.team8.backend.domain.membership.dto.response;

public record MembershipApplyResponse(
        Long applicationId,
        String message
) {
    public static MembershipApplyResponse of(Long applicationId) {
        return new MembershipApplyResponse(applicationId, "블루체크 신청이 완료되었습니다.");
    }
}
