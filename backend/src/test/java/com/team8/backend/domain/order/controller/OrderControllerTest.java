package com.team8.backend.domain.order.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.team8.backend.domain.auction.entity.Auction;
import com.team8.backend.domain.auction.entity.AuctionStatus;
import com.team8.backend.domain.auction.repository.AuctionRepository;
import com.team8.backend.domain.order.dto.request.OrderAddressRequest;
import com.team8.backend.domain.order.entity.Order;
import com.team8.backend.domain.order.repository.OrderRepository;
import com.team8.backend.domain.user.entity.User;
import com.team8.backend.domain.user.entity.UserRole;
import com.team8.backend.domain.user.repository.UserRepository;
import com.team8.backend.global.security.CustomUserDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class OrderControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private AuctionRepository auctionRepository;
    @Autowired private OrderRepository orderRepository;

    // ObjectMapper는 빈 주입 대신 직접 생성하여 의존성 문제를 해결합니다.
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    private CustomUserDetails testUser;
    private Long orderId;

    @BeforeEach
    void setUp() {
        // 1. 테스트 유저 생성
        User user = userRepository.save(User.create("test@t.com", "pw", "테스트유저", "서울"));
        testUser = new CustomUserDetails(user.getUserId(), user.getEmail(), UserRole.ROLE_USER, user.getNickname());

        // 2. 경매 및 주문 생성
        Auction a = auctionRepository.save(Auction.builder()
                .seller(user).title("상품").startPrice(1000L).currentPrice(1000L)
                .bidUnit(100).status(AuctionStatus.WON)
                .startedAt(LocalDateTime.now().minusDays(1)).endedAt(LocalDateTime.now()).build());

        Order o = orderRepository.save(Order.builder()
                .auction(a).bidder(user).seller(user).finalPrice(1000L)
                .shippingAddress("주소").createdAt(LocalDateTime.now()).build());

        this.orderId = o.getOrderId();
    }

    @Test
    @DisplayName("주문 상세 조회 API 테스트")
    void getOrder_ApiTest() throws Exception {
        mockMvc.perform(get("/api/orders/" + orderId)
                        .with(user(testUser)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("배송 정보 입력 API 테스트")
    void updateAddress_ApiTest() throws Exception {
        OrderAddressRequest request = new OrderAddressRequest("서울시 강남구", false);

        mockMvc.perform(post("/api/orders/" + orderId + "/shipping")
                        .with(user(testUser))
                        .with(csrf()) // 보안 설정 때문에 POST에는 CSRF 토큰이 필요합니다.
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("결제 API 테스트")
    void payOrder_ApiTest() throws Exception {
        mockMvc.perform(post("/api/orders/" + orderId + "/pay")
                        .with(user(testUser))
                        .with(csrf()))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("내 거래 내역 조회 API 테스트")
    void getMyOrders_ApiTest() throws Exception {
        mockMvc.perform(get("/api/orders/me")
                        .param("role", "BUYER")
                        .param("filter", "ONGOING")
                        .with(user(testUser)))
                .andExpect(status().isOk());
    }
}
