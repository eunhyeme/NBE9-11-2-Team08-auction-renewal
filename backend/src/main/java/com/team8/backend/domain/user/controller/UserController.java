package com.team8.backend.domain.user.controller;

import com.team8.backend.domain.user.dto.request.LoginRequest;
import com.team8.backend.domain.user.dto.request.SignupRequest;
import com.team8.backend.domain.user.dto.request.UserMeUpdateRequest;
import com.team8.backend.domain.user.dto.response.LoginResponse;
import com.team8.backend.domain.user.dto.response.UserMeResponse;
import com.team8.backend.domain.user.service.UserService;
import com.team8.backend.global.response.ApiResponse;
import com.team8.backend.global.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<Void>> signup(@RequestBody @Valid SignupRequest request) {
        userService.join(request.email(), request.password(), request.nickname(), request.address());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(null));
    }

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@RequestBody @Valid LoginRequest request) {
        String token = userService.login(request.email(), request.password());
        return ApiResponse.success(new LoginResponse(token));
    }

    @Operation(security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping("/me")
    public ApiResponse<UserMeResponse> getMe(@AuthenticationPrincipal CustomUserDetails userDetails) {
        return ApiResponse.success(UserMeResponse.from(userService.getMe(userDetails)));
    }

    @Operation(security = @SecurityRequirement(name = "bearerAuth"))
    @PutMapping("/me")
    public ApiResponse<UserMeResponse> updateMyInfo(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody UserMeUpdateRequest request
    ) {
        return ApiResponse.success(userService.updateMyInfo(userDetails, request));
    }
}
