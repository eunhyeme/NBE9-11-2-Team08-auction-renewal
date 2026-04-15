package com.team8.backend.domain.user.dto.response;

import com.team8.backend.domain.user.entity.User;
import java.time.LocalDateTime;

public record UserMeResponse(
        String email,
        String nickname,
        String address,
        Boolean hasBluecheck,
        LocalDateTime createdAt
) {
    public static UserMeResponse from(User user) {
        return new UserMeResponse(
                user.getEmail(),
                user.getNickname(),
                user.getAddress(),
                user.getHasBluecheck(),
                user.getCreatedAt()
        );
    }
}
