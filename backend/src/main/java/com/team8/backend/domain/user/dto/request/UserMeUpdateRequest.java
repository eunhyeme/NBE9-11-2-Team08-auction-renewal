package com.team8.backend.domain.user.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UserMeUpdateRequest(

        @NotBlank(message = "닉네임은 필수입니다.")
        @Size(max = 50, message = "닉네임은 50자를 초과할 수 없습니다.")
        String nickname,

        @Size(max = 255, message = "주소는 255자를 초과할 수 없습니다.")
        String address
) {
}
