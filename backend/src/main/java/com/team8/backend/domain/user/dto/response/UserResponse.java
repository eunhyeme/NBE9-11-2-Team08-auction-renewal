package com.team8.backend.domain.user.dto.response;

import com.team8.backend.domain.user.entity.User;

public record UserResponse(String email, String nickname, String address) {

    public static UserResponse from(User user) {
        return new UserResponse(user.getEmail(), user.getNickname(), user.getAddress());
    }
}
