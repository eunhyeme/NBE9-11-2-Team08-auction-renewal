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
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test") // ⭐ JPA Auditing 문제 방지용 (중요)
class BidQueryControllerTest {

    @Autowired MockMvc mockMvc;

    @MockitoBean
    BidService bidService;

    @MockitoBean
    JwtProvider jwtProvider;

    private static final Long USER_ID    = 2L;
    private static final Long AUCTION_ID = 1L;

    // ═══════════════════════════════════════════════
    // GET /api/bids/me
    // ═══════════════════════════════════════════════
    @Nested
    @DisplayName("GET /api/bids/me - 내 최신 입찰 목록")
    class GetMyBids {

        @Test
        @DisplayName("정상 조회 → 200 OK, 목록 반환")
        void success() throws Exception {
            List<MyBidResponse> bids = List.of(
                    sampleMyBidResponse(1L, AUCTION_ID, 2000L, true),
                    sampleMyBidResponse(2L, 20L, 3000L, false)
            );
            given(bidService.getMyBids(USER_ID)).willReturn(bids);

            mockMvc.perform(get("/api/bids/me")
                            .with(user(mockUserDetails(USER_ID))))
                    .andDo(print())
                    .andExpect(status().isOk())
                    // 🔥 result로 수정해야 할 수도 있음 (너 API 구조 따라)
                    .andExpect(jsonPath("$.result").isArray())
                    .andExpect(jsonPath("$.result.length()").value(2))
                    .andExpect(jsonPath("$.result[0].myBidPrice").value(2000))
                    .andExpect(jsonPath("$.result[0].isWinning").value(true))
                    .andExpect(jsonPath("$.result[1].isWinning").value(false));
        }

        @Test
        @DisplayName("입찰 이력 없음 → 200 OK, 빈 배열")
        void empty() throws Exception {
            given(bidService.getMyBids(USER_ID)).willReturn(List.of());

            mockMvc.perform(get("/api/bids/me")
                            .with(user(mockUserDetails(USER_ID))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.result").isArray())
                    .andExpect(jsonPath("$.result.length()").value(0));
        }

        @Test
        @DisplayName("미인증 요청 → 401 Unauthorized")
        void unauthorized() throws Exception {
            mockMvc.perform(get("/api/bids/me"))
                    .andExpect(status().isUnauthorized());
        }
    }

    // ═══════════════════════════════════════════════
    // GET /api/bids/me/{auctionId}
    // ═══════════════════════════════════════════════
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
                            .with(user(mockUserDetails(USER_ID))))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.result.length()").value(2))
                    .andExpect(jsonPath("$.result[0].myBidPrice").value(2000))
                    .andExpect(jsonPath("$.result[1].myBidPrice").value(1500));
        }

        @Test
        @DisplayName("해당 경매 이력 없음 → 200 OK, 빈 배열")
        void empty() throws Exception {
            given(bidService.getMyBidHistory(USER_ID, AUCTION_ID)).willReturn(List.of());

            mockMvc.perform(get("/api/bids/me/{auctionId}", AUCTION_ID)
                            .with(user(mockUserDetails(USER_ID))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.result").isArray())
                    .andExpect(jsonPath("$.result.length()").value(0));
        }

        @Test
        @DisplayName("미인증 요청 → 401 Unauthorized")
        void unauthorized() throws Exception {
            mockMvc.perform(get("/api/bids/me/{auctionId}", AUCTION_ID))
                    .andExpect(status().isUnauthorized());
        }
    }

    // ═══════════════════════════════════════════════
    // 헬퍼
    // ═══════════════════════════════════════════════
    private MyBidResponse sampleMyBidResponse(Long bidId, Long auctionId,
                                              Long bidPrice, boolean isWinning) {
        return new MyBidResponse(
                bidId,
                auctionId,
                "테스트 경매",
                "https://example.com/image.jpg",
                bidPrice,
                bidPrice,
                AuctionStatus.ONGOING,
                1L,
                "seller",
                LocalDateTime.now(),
                LocalDateTime.now().plusHours(1),
                isWinning ? 2L : 3L,
                isWinning
        );
    }

    private CustomUserDetails mockUserDetails(Long userId) {
        return new CustomUserDetails(userId, "user@test.com", ROLE_USER,"nickname");
    }
}