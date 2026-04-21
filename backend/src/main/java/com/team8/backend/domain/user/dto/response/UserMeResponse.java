package com.team8.backend.domain.user.dto.response;

import com.team8.backend.domain.user.entity.User;
import com.team8.backend.domain.user.entity.UserRole;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

public record UserMeResponse(
        Long userId,
        String email,
        String nickname,
        String address,
        List<String> permissions,
        UserRole role,
        LocalDateTime createdAt
) {
    public static UserMeResponse from(User user) {
        return new UserMeResponse(
                user.getUserId(),
                user.getEmail(),
                user.getNickname(),
                user.getAddress(),
                user.getPermissions().stream().map(Enum::name).collect(Collectors.toList()),
                user.getRole(),
                user.getCreatedAt()
        );
    }
}
