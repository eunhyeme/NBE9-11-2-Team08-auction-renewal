package com.team8.backend.domain.user.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.team8.backend.domain.user.dto.request.LoginRequest;
import com.team8.backend.domain.user.dto.request.SignupRequest;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class UserControllerTest {

    @Autowired private WebApplicationContext wac;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(wac)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();
    }

    private static final String EMAIL = "test@example.com";
    private static final String PASSWORD = "password123";
    private static final String NICKNAME = "테스터";

    private void signup(String email, String password, String nickname) throws Exception {
        mockMvc.perform(post("/api/users/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new SignupRequest(email, password, nickname, null))))
                .andExpect(status().isCreated());
    }

    private String login(String email, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/users/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new LoginRequest(email, password))))
                .andExpect(status().isOk())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString()).get("result").get("accessToken").asText();
    }

    private Cookie loginAndGetRefreshTokenCookie(String email, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/users/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new LoginRequest(email, password))))
                .andExpect(status().isOk())
                .andReturn();

        return result.getResponse().getCookie("refreshToken");
    }

    @Test
    @DisplayName("회원가입 성공")
    void signup_success() throws Exception {
        mockMvc.perform(post("/api/users/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new SignupRequest(EMAIL, PASSWORD, NICKNAME, null))))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("이메일 형식이 잘못된 경우 회원가입 실패")
    void signup_invalidEmail_fail() throws Exception {
        mockMvc.perform(post("/api/users/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new SignupRequest("not-an-email", PASSWORD, NICKNAME, null))))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @DisplayName("닉네임 없는 경우 회원가입 실패")
    void signup_blankNickname_fail() throws Exception {
        mockMvc.perform(post("/api/users/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new SignupRequest(EMAIL, PASSWORD, "", null))))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @DisplayName("이메일 중복 시 회원가입 실패")
    void signup_duplicateEmail_fail() throws Exception {
        signup(EMAIL, PASSWORD, NICKNAME);

        mockMvc.perform(post("/api/users/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new SignupRequest(EMAIL, PASSWORD, "다른닉네임", null))))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @DisplayName("로그인 성공 시 AccessToken 반환 및 RefreshToken 쿠키 설정")
    void login_success() throws Exception {
        signup(EMAIL, PASSWORD, NICKNAME);

        mockMvc.perform(post("/api/users/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new LoginRequest(EMAIL, PASSWORD))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.result.refreshToken").doesNotExist())
                .andExpect(cookie().exists("refreshToken"))
                .andExpect(cookie().httpOnly("refreshToken", true));
    }

    @Test
    @DisplayName("잘못된 비밀번호로 로그인 실패")
    void login_wrongPassword_fail() throws Exception {
        signup(EMAIL, PASSWORD, NICKNAME);

        mockMvc.perform(post("/api/users/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new LoginRequest(EMAIL, "wrongPassword"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("RefreshToken 쿠키로 새 AccessToken 발급")
    void refresh_success() throws Exception {
        signup(EMAIL, PASSWORD, NICKNAME);
        Cookie refreshTokenCookie = loginAndGetRefreshTokenCookie(EMAIL, PASSWORD);

        mockMvc.perform(post("/api/users/refresh")
                .cookie(refreshTokenCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.accessToken").isNotEmpty());
    }

    @Test
    @DisplayName("RefreshToken 쿠키 없이 재발급 시 400")
    void refresh_noCookie_fail() throws Exception {
        mockMvc.perform(post("/api/users/refresh"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @DisplayName("로그아웃 성공 시 RefreshToken 쿠키 삭제")
    void logout_success() throws Exception {
        signup(EMAIL, PASSWORD, NICKNAME);
        String token = login(EMAIL, PASSWORD);

        mockMvc.perform(post("/api/users/logout")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(cookie().maxAge("refreshToken", 0));
    }

    @Test
    @DisplayName("토큰 없이 로그아웃 시 401")
    void logout_noToken_unauthorized() throws Exception {
        mockMvc.perform(post("/api/users/logout"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("내 정보 조회 성공")
    void getMe_success() throws Exception {
        signup(EMAIL, PASSWORD, NICKNAME);
        String token = login(EMAIL, PASSWORD);

        mockMvc.perform(get("/api/users/me")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.email").value(EMAIL))
                .andExpect(jsonPath("$.result.nickname").value(NICKNAME));
    }

    @Test
    @DisplayName("토큰 없이 내 정보 조회 시 401")
    void getMe_noToken_unauthorized() throws Exception {
        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("유효하지 않은 토큰으로 내 정보 조회 시 401")
    void getMe_invalidToken_unauthorized() throws Exception {
        mockMvc.perform(get("/api/users/me")
                .header("Authorization", "Bearer invalid.token.value"))
                .andExpect(status().isUnauthorized());
    }
}
