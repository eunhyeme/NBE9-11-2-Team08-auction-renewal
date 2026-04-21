package com.team8.backend.domain.auction.controller;

import com.team8.backend.domain.auction.entity.Auction;
import com.team8.backend.domain.auction.entity.AuctionStatus;
import com.team8.backend.domain.auction.repository.AuctionRepository;
import com.team8.backend.domain.user.entity.User;
import com.team8.backend.domain.user.entity.UserPermission;
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
        User sellerUser = User.create("seller@test.com", "pw", "판매자유저", "부산");
        sellerUser.grantPermission(UserPermission.SELLER);
        userRepository.save(sellerUser);

        auctionRepository.save(auction("아이폰 14", 600000L, normalUser));
        auctionRepository.save(auction("맥북 프로", 1200000L, sellerUser));
        auctionRepository.save(auction("에어팟", 150000L, normalUser));
        auctionRepository.save(auction("갤럭시 탭", 300000L, sellerUser));
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

    @Test
    @DisplayName("statuses 미전달 시 ONGOING 경매만 반환")
    void getAuctions_noStatuses_returnsOnlyOngoing() throws Exception {
        User seller = userRepository.save(User.create("s2@test.com", "pw", "판매자2", null));
        auctionRepository.save(auctionWithStatus("낙찰 상품", 500000L, seller, AuctionStatus.WON));
        auctionRepository.save(auctionWithStatus("유찰 상품", 200000L, seller, AuctionStatus.UNSOLD));

        mockMvc.perform(get("/api/auctions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.totalElements", is(4)))
                .andExpect(jsonPath("$.result.content[*].status", everyItem(is("ONGOING"))));
    }

    @Test
    @DisplayName("statuses=WON 이면 낙찰 경매만 반환")
    void getAuctions_statusWon_returnsOnlyWon() throws Exception {
        User seller = userRepository.save(User.create("s3@test.com", "pw", "판매자3", null));
        auctionRepository.save(auctionWithStatus("낙찰 상품", 500000L, seller, AuctionStatus.WON));

        mockMvc.perform(get("/api/auctions").param("statuses", "WON"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.totalElements", is(1)))
                .andExpect(jsonPath("$.result.content[0].status", is("WON")));
    }

    @Test
    @DisplayName("statuses=UNSOLD 이면 유찰 경매만 반환")
    void getAuctions_statusUnsold_returnsOnlyUnsold() throws Exception {
        User seller = userRepository.save(User.create("s4@test.com", "pw", "판매자4", null));
        auctionRepository.save(auctionWithStatus("유찰 상품", 200000L, seller, AuctionStatus.UNSOLD));

        mockMvc.perform(get("/api/auctions").param("statuses", "UNSOLD"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.totalElements", is(1)))
                .andExpect(jsonPath("$.result.content[0].status", is("UNSOLD")));
    }

    @Test
    @DisplayName("statuses=CANCELED 전달 시 CANCELED는 무시되어 ONGOING만 반환")
    void getAuctions_statusCanceled_ignoredAndReturnsOngoing() throws Exception {
        mockMvc.perform(get("/api/auctions").param("statuses", "CANCELED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.totalElements", is(4)))
                .andExpect(jsonPath("$.result.content[*].status", everyItem(is("ONGOING"))));
    }

    private Auction auctionWithStatus(String title, Long price, User seller, AuctionStatus status) {
        return Auction.builder()
                .seller(seller)
                .title(title)
                .imageUrl("/img.jpg")
                .description("설명")
                .startPrice(price)
                .currentPrice(price)
                .bidUnit(1000)
                .status(status)
                .startedAt(NOW.plusDays(1))
                .endedAt(NOW.plusDays(3))
                .build();
    }
}
