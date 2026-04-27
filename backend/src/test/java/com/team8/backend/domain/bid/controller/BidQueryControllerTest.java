package com.team8.backend.domain.bid.controller;

import com.team8.backend.domain.auction.entity.AuctionStatus;
import com.team8.backend.domain.bid.dto.MyBidResponse;
import com.team8.backend.domain.bid.service.BidService;
import com.team8.backend.global.security.CustomUserDetails;
import com.team8.backend.global.security.JwtProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static com.team8.backend.domain.user.entity.UserRole.ROLE_USER;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class BidQueryControllerTest { // 💡 IntegrationTest 상속 제거 (Mocking 기반이므로)

    @Autowired MockMvc mockMvc;

    @MockitoBean BidService bidService;
    @MockitoBean JwtProvider jwtProvider;

    private static final Long USER_ID    = 2L;
    private static final Long AUCTION_ID = 1L;

    @Nested
    @DisplayName("GET /api/bids/me - 내 최신 입찰 목록")
    class GetMyBids {

        @Test
        @DisplayName("정상 조회 → 200 OK, 목록 반환")
        void success() throws Exception {
            // Given: USER_ID(2)가 마지막 입찰자인 경우(true)와 아닌 경우(false)
            List<MyBidResponse> bids = List.of(
                    sampleMyBidResponse(1L, AUCTION_ID, 2000L, true),
                    sampleMyBidResponse(2L, 20L, 3000L, false)
            );
            given(bidService.getMyBids(USER_ID)).willReturn(bids);

            // When & Then
            mockMvc.perform(get("/api/bids/me")
                            .with(user(mockUserDetails(USER_ID)))
                            .with(csrf()))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.result").isArray()) // 💡 result 필드 확인
                    .andExpect(jsonPath("$.result.length()").value(2))
                    .andExpect(jsonPath("$.result[0].myBidPrice").value(2000))
                    // 💡 isWinning 대신 lastBidderId로 검증 (DTO 구조에 맞춤)
                    .andExpect(jsonPath("$.result[0].lastBidderId").value(USER_ID))
                    .andExpect(jsonPath("$.result[1].lastBidderId").value(3L));
        }

        @Test
        @DisplayName("입찰 이력 없음 → 200 OK, 빈 배열")
        void empty() throws Exception {
            given(bidService.getMyBids(USER_ID)).willReturn(List.of());

            mockMvc.perform(get("/api/bids/me")
                            .with(user(mockUserDetails(USER_ID)))
                            .with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.result").isArray())
                    .andExpect(jsonPath("$.result.length()").value(0));
        }
    }

    @Nested
    @DisplayName("GET /api/bids/me/{auctionId} - 특정 경매 내 입찰 이력")
    class GetMyBidHistory {

        @Test
        @DisplayName("정상 조회 → 200 OK, 이력 반환")
        void success() throws Exception {
            List<MyBidResponse> history = List.of(
                    sampleMyBidResponse(3L, AUCTION_ID, 2000L, true),
                    sampleMyBidResponse(4L, AUCTION_ID, 1500L, false)
            );
            given(bidService.getMyBidHistory(USER_ID, AUCTION_ID)).willReturn(history);

            mockMvc.perform(get("/api/bids/me/{auctionId}", AUCTION_ID)
                            .with(user(mockUserDetails(USER_ID)))
                            .with(csrf()))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.result.length()").value(2))
                    .andExpect(jsonPath("$.result[0].myBidPrice").value(2000))
                    .andExpect(jsonPath("$.result[1].myBidPrice").value(1500));
        }
    }

    // ═══════════════════════════════════════════════
    // 헬퍼 메서드
    // ═══════════════════════════════════════════════
    private MyBidResponse sampleMyBidResponse(Long bidId, Long auctionId,
                                              Long bidPrice, boolean isWinning) {
        return new MyBidResponse(
                bidId,
                auctionId,
                "테스트 경매",
                "https://example.com/image.jpg",
                bidPrice,
                bidPrice, // currentPrice
                AuctionStatus.ONGOING,
                1L, // sellerId
                "seller",
                LocalDateTime.now(),
                LocalDateTime.now().plusHours(1),
                isWinning ? USER_ID : 3L // 💡 USER_ID(2)면 승리 중인 것으로 설정
        );
    }

    private CustomUserDetails mockUserDetails(Long userId) {
        return new CustomUserDetails(userId, "user@test.com", ROLE_USER, "nickname");
    }
}
