package com.team8.backend.domain.bid.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.team8.backend.domain.bid.dto.BidRequest;
import com.team8.backend.domain.bid.dto.BidResponse;
import com.team8.backend.domain.bid.service.BidService;
import com.team8.backend.global.security.CustomUserDetails;
import com.team8.backend.global.security.JwtProvider; // 👈 JwtProvider 추가
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static com.team8.backend.domain.user.entity.UserRole.ROLE_USER;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class BidControllerTest {

    @Autowired MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @MockitoBean BidService bidService;
    @MockitoBean JwtProvider jwtProvider; // 💡 시큐리티 필터 우회용

    private static final Long USER_ID    = 2L;
    private static final Long AUCTION_ID = 10L;
    private static final String NICKNAME = "사용자22";

    @Nested
    @DisplayName("POST /api/auctions/bids - 입찰 생성")
    class CreateBid {
        @Test
        @DisplayName("정상 입찰 → 200 OK")
        void success() throws Exception {
            BidRequest req = new BidRequest(AUCTION_ID, 2000L);
            BidResponse res = new BidResponse(AUCTION_ID, 2000L, USER_ID, LocalDateTime.now(), NICKNAME);

            given(bidService.createBid(eq(USER_ID), any(BidRequest.class))).willReturn(res);

            mockMvc.perform(post("/api/auctions/bids")
                            .with(csrf())
                            .with(user(mockUserDetails()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.result.auctionId").value(AUCTION_ID))
                    .andExpect(jsonPath("$.result.bidPrice").value(2000));
        }
    }

    @Nested
    @DisplayName("GET 입찰 목록")
    class GetAuctionBids {
        @Test
        @DisplayName("성공적으로 목록을 조회한다")
        void success() throws Exception {
            List<BidResponse> bids = List.of(
                    new BidResponse(AUCTION_ID, 2000L, 2L, LocalDateTime.now(), NICKNAME),
                    new BidResponse(AUCTION_ID, 1500L, 3L, LocalDateTime.now(), NICKNAME)
            );

            given(bidService.getAuctionBids(AUCTION_ID)).willReturn(bids);

            mockMvc.perform(get("/api/auctions/{auctionId}/bids", AUCTION_ID)
                            .with(user(mockUserDetails())))
                    .andDo(print())
                    .andExpect(status().isOk())
                    // 💡 $.data -> $.result 로 수정 (로그에 찍힌 결과 확인됨)
                    .andExpect(jsonPath("$.result.length()").value(2));
        }
    }

    private CustomUserDetails mockUserDetails() {
        return new CustomUserDetails(USER_ID, "user@test.com", ROLE_USER, NICKNAME);
    }
}
