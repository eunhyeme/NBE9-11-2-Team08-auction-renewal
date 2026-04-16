package com.team8.backend.domain.user.dto.response;

import com.team8.backend.domain.user.entity.User;
import com.team8.backend.domain.user.entity.UserRole;
import java.time.LocalDateTime;

public record UserMeResponse(
        Long userId,
        String email,
        String nickname,
        String address,
        Boolean hasBluecheck,
        UserRole role,
        LocalDateTime createdAt
) {
    public static UserMeResponse from(User user) {
        return new UserMeResponse(
                user.getUserId(),
                user.getEmail(),
                user.getNickname(),
                user.getAddress(),
                user.getHasBluecheck(),
                user.getRole(),
                user.getCreatedAt()
        );
    }
}
