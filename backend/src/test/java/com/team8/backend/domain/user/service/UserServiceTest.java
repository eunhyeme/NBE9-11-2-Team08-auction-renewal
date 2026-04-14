package com.team8.backend.domain.user.service;

import com.team8.backend.domain.user.entity.User;
import com.team8.backend.domain.user.repository.UserRepository;
import com.team8.backend.global.security.CustomUserDetails;
import com.team8.backend.global.security.JwtProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private JwtProvider jwtProvider;

    @InjectMocks private UserService userService;

    @Test
    @DisplayName("회원가입 성공")
    void join_success() {
        given(userRepository.findByEmail(anyString())).willReturn(Optional.empty());
        given(passwordEncoder.encode(anyString())).willReturn("encodedPassword");
        given(userRepository.save(any(User.class)))
                .willReturn(User.create("test@example.com", "encodedPassword", "테스터", null));

        userService.join("test@example.com", "password", "테스터", null);

        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("이메일 중복 시 예외 발생")
    void join_duplicateEmail_throwsException() {
        given(userRepository.findByEmail("test@example.com"))
                .willReturn(Optional.of(User.create("test@example.com", "pw", "nick", null)));

        assertThatThrownBy(() -> userService.join("test@example.com", "password", "테스터", null))
                .isInstanceOf(RuntimeException.class);

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("로그인 성공 시 JWT 토큰 반환")
    void login_success() {
        Authentication authentication = new UsernamePasswordAuthenticationToken("test@example.com", "password");
        given(authenticationManager.authenticate(any())).willReturn(authentication);
        given(jwtProvider.generateToken(authentication)).willReturn("jwt-token");

        String token = userService.login("test@example.com", "password");

        assertThat(token).isEqualTo("jwt-token");
    }

    @Test
    @DisplayName("잘못된 비밀번호로 로그인 시 예외 발생")
    void login_wrongPassword_throwsException() {
        given(authenticationManager.authenticate(any()))
                .willThrow(new BadCredentialsException("잘못된 인증 정보"));

        assertThatThrownBy(() -> userService.login("test@example.com", "wrongPassword"))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    @DisplayName("내 정보 조회 성공")
    void getMe_success() {
        User user = User.create("test@example.com", "pw", "테스터", "서울");
        CustomUserDetails userDetails = new CustomUserDetails(user);
        given(userRepository.findByEmail("test@example.com")).willReturn(Optional.of(user));

        User result = userService.getMe(userDetails);

        assertThat(result.getEmail()).isEqualTo("test@example.com");
        assertThat(result.getNickname()).isEqualTo("테스터");
    }

    @Test
    @DisplayName("존재하지 않는 사용자 조회 시 예외 발생")
    void getMe_userNotFound_throwsException() {
        CustomUserDetails userDetails = new CustomUserDetails(
                User.create("ghost@example.com", "pw", "ghost", null)
        );
        given(userRepository.findByEmail(anyString())).willReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getMe(userDetails))
                .isInstanceOf(UsernameNotFoundException.class);
    }
}
