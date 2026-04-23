package com.team8.backend.domain.order.scheduler;

import com.team8.backend.domain.auction.entity.Auction;
import com.team8.backend.domain.auction.entity.AuctionStatus;
import com.team8.backend.domain.auction.repository.AuctionRepository;
import com.team8.backend.domain.order.entity.Order;
import com.team8.backend.domain.order.entity.OrderStatus;
import com.team8.backend.domain.order.repository.OrderRepository;
import com.team8.backend.domain.user.entity.User;
import com.team8.backend.domain.user.repository.UserRepository;
import jakarta.persistence.EntityManager;
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
class OrderSchedulerIntegrationTest {

    @Autowired private OrderScheduler orderScheduler;
    @Autowired private OrderRepository orderRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private AuctionRepository auctionRepository;
    @Autowired private PlatformTransactionManager tm;
    @Autowired private EntityManager em;

    private User seller;
    private User bidder;

    @BeforeEach
    void setUp() {
        TransactionStatus deleteStatus = tm.getTransaction(new DefaultTransactionDefinition());
        orderRepository.deleteAllInBatch();
        auctionRepository.deleteAllInBatch();
        userRepository.deleteAllInBatch();
        tm.commit(deleteStatus);

        // 2. 기본 사용자 데이터 생성 및 물리 커밋
        TransactionStatus createStatus = tm.getTransaction(new DefaultTransactionDefinition());
        seller = userRepository.save(User.create("seller@test.com", "pw", "판매자", "주소"));
        bidder = userRepository.save(User.create("bidder@test.com", "pw", "구매자", "주소"));
        tm.commit(createStatus);
    }

    @Test
    @DisplayName("결제 및 출고 기한이 만료된 주문들이 스케줄러에 의해 일괄 처리된다")
    void handleOrderTimeouts_Success() {
        // Given: 테스트용 데이터 생성
        TransactionStatus status = tm.getTransaction(new DefaultTransactionDefinition());

        // (1) 결제 기한 만료 케이스: 49시간 전 생성된 주문
        Auction a1 = createAuction("상품1");
        Order o1 = orderRepository.save(Order.builder()
                .auction(a1).bidder(bidder).seller(seller).finalPrice(1000L).shippingAddress("서울")
                .createdAt(LocalDateTime.now().minusHours(49)).build());

        // (2) 출고 기한 만료 케이스: 73시간 전 결제 완료된 주문
        Auction a2 = createAuction("상품2");
        Order o2 = Order.builder()
                .auction(a2).bidder(bidder).seller(seller).finalPrice(1000L).shippingAddress("서울")
                .createdAt(LocalDateTime.now().minusHours(100)).build();
        o2.pay(LocalDateTime.now().minusHours(73)); // 결제 시점 설정
        o2 = orderRepository.save(o2);

        // (3) 정상 주문 케이스: 현재 시점에 생성된 주문
        Auction a3 = createAuction("상품3");
        Order o3 = orderRepository.save(Order.builder()
                .auction(a3).bidder(bidder).seller(seller).finalPrice(1000L).shippingAddress("서울")
                .createdAt(LocalDateTime.now()).build());

        tm.commit(status);

        // When: 스케줄러 실행
        orderScheduler.handleOrderTimeouts();
        em.clear();

        // Then: 각 주문의 상태 변화 검증
        Order result1 = orderRepository.findById(o1.getOrderId()).orElseThrow();
        Order result2 = orderRepository.findById(o2.getOrderId()).orElseThrow();
        Order result3 = orderRepository.findById(o3.getOrderId()).orElseThrow();

        assertThat(result1.getStatus()).isEqualTo(OrderStatus.NOT_PAID);       // 48시간 초과 -> 미결제 처리
        assertThat(result2.getStatus()).isEqualTo(OrderStatus.NOT_DELIVERED); // 72시간 초과 -> 미출고 처리
        assertThat(result3.getStatus()).isEqualTo(OrderStatus.AWAITING_PAYMENT); // 기한 내 -> 상태 유지
    }

    private Auction createAuction(String title) {
        return auctionRepository.save(Auction.builder()
                .seller(seller).title(title).startPrice(100L).currentPrice(100L).bidUnit(10)
                .status(AuctionStatus.WON).startedAt(LocalDateTime.now()).endedAt(LocalDateTime.now()).build());
    }
}
