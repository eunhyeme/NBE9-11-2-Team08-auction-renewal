package com.team8.backend.global.security;

import com.team8.backend.domain.user.entity.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import static org.assertj.core.api.Assertions.assertThat;

class JwtProviderTest {

    private static final String SECRET = "test-secret-key-that-is-at-least-32-characters-long-for-hs256";
    private JwtProvider jwtProvider;

    @BeforeEach
    void setUp() {
        jwtProvider = new JwtProvider(SECRET, 1800);
    }

    private Authentication createAuthentication() {
        CustomUserDetails userDetails = new CustomUserDetails(1L, "test@example.com", UserRole.ROLE_USER, "test");
        return new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
    }

    @Test
    @DisplayName("토큰 생성 성공")
    void generateToken_success() {
        String token = jwtProvider.generateToken(createAuthentication());

        assertThat(token).isNotBlank();
    }

    @Test
    @DisplayName("유효한 토큰 검증 성공")
    void validateToken_validToken() {
        String token = jwtProvider.generateToken(createAuthentication());

        assertThat(jwtProvider.validateToken(token)).isTrue();
    }

    @Test
    @DisplayName("변조된 토큰 검증 실패")
    void validateToken_tamperedToken() {
        assertThat(jwtProvider.validateToken("invalid.token.value")).isFalse();
    }

    @Test
    @DisplayName("만료된 토큰 검증 실패")
    void validateToken_expiredToken() throws InterruptedException {
        JwtProvider shortLivedProvider = new JwtProvider(SECRET, 1);
        String token = shortLivedProvider.generateToken(createAuthentication());

        Thread.sleep(1100);

        assertThat(shortLivedProvider.validateToken(token)).isFalse();
    }

    @Test
    @DisplayName("리프레시 토큰 생성 성공")
    void generateRefreshToken_success() {
        String token = jwtProvider.generateRefreshToken(createAuthentication());

        assertThat(token).isNotBlank();
    }

    @Test
    @DisplayName("리프레시 토큰은 만료되지 않음")
    void generateRefreshToken_doesNotExpire() throws InterruptedException {
        JwtProvider shortLivedProvider = new JwtProvider(SECRET, 1);
        String refreshToken = shortLivedProvider.generateRefreshToken(createAuthentication());

        Thread.sleep(1100);

        assertThat(shortLivedProvider.validateToken(refreshToken)).isTrue();
    }

    @Test
    @DisplayName("리프레시 토큰에서 인증 정보 추출")
    void getAuthentication_fromRefreshToken() {
        String refreshToken = jwtProvider.generateRefreshToken(createAuthentication());

        Authentication authentication = jwtProvider.getAuthentication(refreshToken);
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();

        assertThat(userDetails.getUsername()).isEqualTo("test@example.com");
        assertThat(userDetails.getUserId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("토큰에서 인증 정보 추출")
    void getAuthentication_success() {
        String token = jwtProvider.generateToken(createAuthentication());

        Authentication authentication = jwtProvider.getAuthentication(token);
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();

        assertThat(userDetails.getUsername()).isEqualTo("test@example.com");
        assertThat(userDetails.getUserId()).isEqualTo(1L);
        assertThat(userDetails.getAuthorities()).hasSize(1);
        assertThat(userDetails.getAuthorities().iterator().next().getAuthority()).isEqualTo("ROLE_USER");
    }
}
