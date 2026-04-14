package com.team8.backend.global.response;

public record ApiResponse<T>(
        String code,
        String message,
        T result
) {

    public static <T> ApiResponse<T> success(T result) {
        return new ApiResponse<>("200-1", "요청 성공", result);
    }

    public static ApiResponse<Void> error(String code, String message) {
        return new ApiResponse<>(code, message, null);
    }
}