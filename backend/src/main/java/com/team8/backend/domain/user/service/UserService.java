package com.team8.backend.domain.user.service;

import com.team8.backend.domain.user.dto.request.UserMeUpdateRequest;
import com.team8.backend.domain.user.dto.response.TokenPair;
import com.team8.backend.domain.user.dto.response.UserMeResponse;
import com.team8.backend.domain.user.entity.User;
import com.team8.backend.domain.user.repository.UserRepository;
import com.team8.backend.global.exception.CustomException;
import com.team8.backend.global.exception.ErrorCode;
import com.team8.backend.global.security.CustomUserDetails;
import com.team8.backend.global.security.JwtProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtProvider jwtProvider;

    @Transactional
    public User join(String email, String password, String nickname, String address) {
        userRepository.findByEmail(email)
                .ifPresent(_ -> {
                    throw new CustomException(ErrorCode.DUPLICATE_EMAIL);
                });
        User user = User.create(email, passwordEncoder.encode(password), nickname, address);
        return userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public TokenPair login(String email, String password) {
        UsernamePasswordAuthenticationToken authToken =
                new UsernamePasswordAuthenticationToken(email, password);
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(authToken);
        } catch (AuthenticationException e) {
            throw new CustomException(ErrorCode.INVALID_CREDENTIAL);
        }
        String accessToken = jwtProvider.generateToken(authentication);
        String refreshToken = jwtProvider.generateRefreshToken(authentication);
        return new TokenPair(accessToken, refreshToken);
    }

    public String refresh(String refreshToken) {
        if (!jwtProvider.validateToken(refreshToken)) {
            throw new CustomException(ErrorCode.UNAUTHORIZED);
        }
        Authentication authentication = jwtProvider.getAuthentication(refreshToken);
        return jwtProvider.generateToken(authentication);
    }

    @Transactional(readOnly = true)
    public User getMe(CustomUserDetails userDetails) {
        return userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
    }

    @Transactional
    public UserMeResponse updateMyInfo(CustomUserDetails userDetails, UserMeUpdateRequest request) {
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        user.updateProfile(request.nickname(), request.address());

        return UserMeResponse.from(user);
    }
}
