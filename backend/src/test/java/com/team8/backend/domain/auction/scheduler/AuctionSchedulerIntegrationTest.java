package com.team8.backend.domain.auction.scheduler;

import com.team8.backend.domain.auction.entity.Auction;
import com.team8.backend.domain.auction.entity.AuctionStatus;
import com.team8.backend.domain.auction.repository.AuctionRepository;
import com.team8.backend.domain.order.repository.OrderRepository;
import com.team8.backend.domain.user.entity.User;
import com.team8.backend.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class AuctionSchedulerIntegrationTest {

    @Autowired private AuctionScheduler auctionScheduler;
    @Autowired private AuctionRepository auctionRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private OrderRepository orderRepository;
    @Autowired private PlatformTransactionManager tm;

    private User seller;
    private User bidder;

    @BeforeEach
    void setUp() {
        TransactionStatus status = tm.getTransaction(new DefaultTransactionDefinition());
        orderRepository.deleteAll();
        auctionRepository.deleteAll();
        userRepository.deleteAll();

        seller = userRepository.save(User.create("seller@a.com", "pw", "판매자", "서울"));
        bidder = userRepository.save(User.create("bidder@a.com", "pw", "입찰자", "서울"));
        tm.commit(status);
    }

    @Test
    @DisplayName("경매 시간이 종료되면 낙찰자 유무에 따라 WON/UNSOLD로 변경되고 주문이 생성된다")
    void processExpiredAuctions_Success() {
        // Given
        TransactionStatus status = tm.getTransaction(new DefaultTransactionDefinition());

        // 1. 낙찰될 경매 (종료시간 지남, 입찰자 있음)
        Auction wonAuction = auctionRepository.save(Auction.builder()
                .seller(seller).lastBidder(bidder).title("낙찰상품").startPrice(100L).currentPrice(1000L)
                .bidUnit(10).status(AuctionStatus.ONGOING).startedAt(LocalDateTime.now().minusDays(1))
                .endedAt(LocalDateTime.now().minusMinutes(1)).build());

        // 2. 유찰될 경매 (종료시간 지남, 입찰자 없음)
        Auction unsoldAuction = auctionRepository.save(Auction.builder()
                .seller(seller).title("유찰상품").startPrice(100L).currentPrice(100L)
                .bidUnit(10).status(AuctionStatus.ONGOING).startedAt(LocalDateTime.now().minusDays(1))
                .endedAt(LocalDateTime.now().minusMinutes(1)).build());

        // 3. 진행 중인 경매 (종료시간 아직 안 지남)
        Auction ongoingAuction = auctionRepository.save(Auction.builder()
                .seller(seller).title("진행상품").startPrice(100L).currentPrice(100L)
                .bidUnit(10).status(AuctionStatus.ONGOING).startedAt(LocalDateTime.now())
                .endedAt(LocalDateTime.now().plusDays(1)).build());

        tm.commit(status);

        // When: 스케줄러 실행
        auctionScheduler.processExpiredAuctions();

        // Then
        assertThat(auctionRepository.findById(wonAuction.getAuctionId()).get().getStatus()).isEqualTo(AuctionStatus.WON);
        assertThat(auctionRepository.findById(unsoldAuction.getAuctionId()).get().getStatus()).isEqualTo(AuctionStatus.UNSOLD);
        assertThat(auctionRepository.findById(ongoingAuction.getAuctionId()).get().getStatus()).isEqualTo(AuctionStatus.ONGOING);

        // 낙찰된 경우 주문 1건 생성 확인
        assertThat(orderRepository.findAll()).hasSize(1);
        assertThat(orderRepository.findAll().get(0).getAuction().getAuctionId()).isEqualTo(wonAuction.getAuctionId());
    }
}
