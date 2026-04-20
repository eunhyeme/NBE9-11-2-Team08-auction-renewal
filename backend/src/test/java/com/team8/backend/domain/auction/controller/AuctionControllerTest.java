package com.team8.backend.domain.auction.controller;

import com.team8.backend.domain.auction.entity.Auction;
import com.team8.backend.domain.auction.entity.AuctionStatus;
import com.team8.backend.domain.auction.repository.AuctionRepository;
import com.team8.backend.domain.user.entity.User;
import com.team8.backend.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AuctionControllerTest {

    @Autowired private WebApplicationContext wac;
    @Autowired private UserRepository userRepository;
    @Autowired private AuctionRepository auctionRepository;

    private MockMvc mockMvc;

    private static final LocalDateTime NOW = LocalDateTime.now();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(wac)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();

        User normalUser = userRepository.save(User.create("normal@test.com", "pw", "일반유저", "서울"));
        User bluecheckUser = userRepository.save(User.createAdmin("blue@test.com", "pw", "블루체크유저", "부산"));

        auctionRepository.save(auction("아이폰 14", 600000L, normalUser));
        auctionRepository.save(auction("맥북 프로", 1200000L, bluecheckUser));
        auctionRepository.save(auction("에어팟", 150000L, normalUser));
        auctionRepository.save(auction("갤럭시 탭", 300000L, bluecheckUser));
    }

    private Auction auction(String title, Long price, User seller) {
        return Auction.builder()
                .seller(seller)
                .title(title)
                .imageUrl("/img.jpg")
                .description("설명")
                .startPrice(price)
                .currentPrice(price)
                .bidUnit(1000)
                .status(AuctionStatus.ONGOING)
                .startedAt(NOW.plusDays(1))
                .endedAt(NOW.plusDays(3))
                .build();
    }

    @Test
    @DisplayName("필터 없이 전체 경매 목록 조회")
    void getAuctions_noFilter_returnsAll() throws Exception {
        mockMvc.perform(get("/api/auctions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.totalElements", is(4)));
    }

    @Test
    @DisplayName("키워드로 제목 검색")
    void getAuctions_keyword_returnsMatchingTitle() throws Exception {
        mockMvc.perform(get("/api/auctions").param("keyword", "아이폰"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.totalElements", is(1)))
                .andExpect(jsonPath("$.result.content[0].title", is("아이폰 14")));
    }

    @Test
    @DisplayName("키워드가 빈 문자열이면 전체 반환")
    void getAuctions_blankKeyword_returnsAll() throws Exception {
        mockMvc.perform(get("/api/auctions").param("keyword", ""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.totalElements", is(4)));
    }

    @Test
    @DisplayName("minPrice 필터로 최소 가격 이상 경매 조회")
    void getAuctions_minPrice_returnsAboveMinPrice() throws Exception {
        mockMvc.perform(get("/api/auctions").param("minPrice", "300000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.totalElements", is(3)))
                .andExpect(jsonPath("$.result.content[*].currentPrice", everyItem(greaterThanOrEqualTo(300000))));
    }

    @Test
    @DisplayName("maxPrice 필터로 최대 가격 이하 경매 조회")
    void getAuctions_maxPrice_returnsBelowMaxPrice() throws Exception {
        mockMvc.perform(get("/api/auctions").param("maxPrice", "300000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.totalElements", is(2)))
                .andExpect(jsonPath("$.result.content[*].currentPrice", everyItem(lessThanOrEqualTo(300000))));
    }

    @Test
    @DisplayName("minPrice + maxPrice 범위 필터")
    void getAuctions_priceRange_returnsInRange() throws Exception {
        mockMvc.perform(get("/api/auctions").param("minPrice", "200000").param("maxPrice", "700000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.totalElements", is(2)))
                .andExpect(jsonPath("$.result.content[*].title", containsInAnyOrder("아이폰 14", "갤럭시 탭")));
    }

    @Test
    @DisplayName("bluecheckOnly=true 이면 블루체크 판매자 경매만 반환")
    void getAuctions_bluecheckOnly_returnsBluecheckSellerAuctions() throws Exception {
        mockMvc.perform(get("/api/auctions").param("bluecheckOnly", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.totalElements", is(2)))
                .andExpect(jsonPath("$.result.content[*].title", containsInAnyOrder("맥북 프로", "갤럭시 탭")));
    }

    @Test
    @DisplayName("bluecheckOnly=false 이면 전체 반환")
    void getAuctions_bluecheckOnlyFalse_returnsAll() throws Exception {
        mockMvc.perform(get("/api/auctions").param("bluecheckOnly", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.totalElements", is(4)));
    }

    @Test
    @DisplayName("keyword + bluecheckOnly 복합 필터 — 일치하는 항목 있음")
    void getAuctions_keywordAndBluecheckOnly_returnsMatching() throws Exception {
        mockMvc.perform(get("/api/auctions").param("keyword", "맥북").param("bluecheckOnly", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.totalElements", is(1)))
                .andExpect(jsonPath("$.result.content[0].title", is("맥북 프로")));
    }

    @Test
    @DisplayName("keyword + bluecheckOnly 복합 필터 — 일치하는 항목 없음")
    void getAuctions_keywordAndBluecheckOnly_returnsEmpty() throws Exception {
        mockMvc.perform(get("/api/auctions").param("keyword", "에어팟").param("bluecheckOnly", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.totalElements", is(0)));
    }

    @Test
    @DisplayName("가격 높은순 정렬")
    void getAuctions_sortByPriceDesc_returnsSortedByPrice() throws Exception {
        mockMvc.perform(get("/api/auctions").param("sort", "currentPrice,desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.content[0].title", is("맥북 프로")))
                .andExpect(jsonPath("$.result.content[1].title", is("아이폰 14")));
    }

    @Test
    @DisplayName("가격 낮은순 정렬")
    void getAuctions_sortByPriceAsc_returnsSortedByPrice() throws Exception {
        mockMvc.perform(get("/api/auctions").param("sort", "currentPrice,asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.content[0].title", is("에어팟")))
                .andExpect(jsonPath("$.result.content[1].title", is("갤럭시 탭")));
    }

    @Test
    @DisplayName("deletedAt이 있는 경매는 결과에서 제외")
    void getAuctions_excludesDeletedAuctions() throws Exception {
        User seller = userRepository.save(User.create("del@test.com", "pw", "삭제유저", null));
        Auction deleted = Auction.builder()
                .seller(seller)
                .title("삭제된 상품")
                .imageUrl("/img.jpg")
                .description("삭제됨")
                .startPrice(100000L)
                .currentPrice(100000L)
                .bidUnit(1000)
                .status(AuctionStatus.CANCELED)
                .startedAt(NOW.plusDays(1))
                .endedAt(NOW.plusDays(3))
                .build();
        deleted.withdraw();
        auctionRepository.save(deleted);

        mockMvc.perform(get("/api/auctions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.totalElements", is(4)));
    }
}
