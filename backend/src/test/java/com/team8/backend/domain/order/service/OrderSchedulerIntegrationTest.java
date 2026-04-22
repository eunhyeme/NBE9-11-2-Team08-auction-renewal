package com.team8.backend.domain.order.service;

import com.team8.backend.domain.auction.entity.Auction;
import com.team8.backend.domain.auction.entity.AuctionStatus;
import com.team8.backend.domain.auction.repository.AuctionRepository;
import com.team8.backend.domain.order.entity.Order;
import com.team8.backend.domain.order.entity.OrderStatus;
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
public class OrderSchedulerIntegrationTest {

    @Autowired private OrderService orderService;
    @Autowired private OrderRepository orderRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private AuctionRepository auctionRepository;
    @Autowired private PlatformTransactionManager tm;

    @BeforeEach
    void cleanUp() {
        // 각 테스트 시작 전 DB를 깨끗하게 비웁니다.
        TransactionStatus status = tm.getTransaction(new DefaultTransactionDefinition());
        orderRepository.deleteAll();
        auctionRepository.deleteAll();
        userRepository.deleteAll();
        tm.commit(status);
    }

    @Test
    @DisplayName("결제 기한(48시간) 만료 시 NOT_PAID로 자동 변경된다")
    void notPaidOrder_WithTimeExpired() {
        // 1. 데이터 준비 및 물리 커밋 (o = save(o) 필수)
        TransactionStatus status = tm.getTransaction(new DefaultTransactionDefinition());
        User s = userRepository.save(User.create("s1@t.com", "p", "S1", "A"));
        User b = userRepository.save(User.create("b1@t.com", "p", "B1", "A"));
        Auction a = auctionRepository.save(Auction.builder()
                .seller(s).title("T").startPrice(100L).currentPrice(10000L)
                .bidUnit(10).status(AuctionStatus.WON)
                .startedAt(LocalDateTime.now().minusDays(3)).endedAt(LocalDateTime.now().minusDays(2))
                .lastBidder(b).build());

        Order o = Order.builder()
                .auction(a).bidder(b).seller(s)
                .finalPrice(10000L).shippingAddress("서울시")
                .createdAt(LocalDateTime.now().minusHours(49)).build();
        o = orderRepository.save(o); // ID 할당
        tm.commit(status); // DB에 완전히 저장하여 REQUIRES_NEW 트랜잭션이 볼 수 있게 함

        // 2. 실행
        orderService.notPaidOrder(o.getOrderId());

        // 3. 검증
        Order updatedOrder = orderRepository.findById(o.getOrderId()).orElseThrow();
        assertThat(updatedOrder.getStatus()).isEqualTo(OrderStatus.NOT_PAID);
    }

    @Test
    @DisplayName("출고 기한(72시간) 만료 시 NOT_DELIVERED로 자동 변경된다")
    void notDeliveredOrder_WithTimeExpired() {
        // 1. 데이터 준비 및 물리 커밋
        TransactionStatus status = tm.getTransaction(new DefaultTransactionDefinition());
        User s = userRepository.save(User.create("s2@t.com", "p", "S2", "A"));
        User b = userRepository.save(User.create("b2@t.com", "p", "B2", "A"));
        Auction a = auctionRepository.save(Auction.builder()
                .seller(s).title("T").startPrice(100L).currentPrice(10000L)
                .bidUnit(10).status(AuctionStatus.WON)
                .startedAt(LocalDateTime.now().minusDays(5)).endedAt(LocalDateTime.now().minusDays(4))
                .lastBidder(b).build());

        Order o = Order.builder()
                .auction(a).bidder(b).seller(s)
                .finalPrice(10000L).shippingAddress("서울시")
                .createdAt(LocalDateTime.now().minusHours(73)).build();

        o.pay(LocalDateTime.now().minusHours(73)); // 상태를 PAID로 만듦
        o = orderRepository.save(o); // ID 할당 및 상태 저장
        tm.commit(status);

        // 2. 실행
        orderService.notDeliveredOrder(o.getOrderId());

        // 3. 검증
        Order updatedOrder = orderRepository.findById(o.getOrderId()).orElseThrow();
        assertThat(updatedOrder.getStatus()).isEqualTo(OrderStatus.NOT_DELIVERED);
    }

    @Test
    @DisplayName("독립적인 트랜잭션(REQUIRES_NEW) 전파가 정상 작동하는지 확인")
    void checkRequiresNewPropagation() {
        // 1. 데이터 생성 후 커밋
        TransactionStatus status = tm.getTransaction(new DefaultTransactionDefinition());
        User s = userRepository.save(User.create("s3@t.com", "p", "S3", "A"));
        User b = userRepository.save(User.create("b3@t.com", "p", "B3", "A"));
        Auction a = auctionRepository.save(Auction.builder()
                .seller(s).title("T").startPrice(100L).currentPrice(100L)
                .bidUnit(10).status(AuctionStatus.WON).startedAt(LocalDateTime.now()).endedAt(LocalDateTime.now()).build());

        Order o = orderRepository.save(Order.builder()
                .auction(a).bidder(b).seller(s)
                .finalPrice(1000L).shippingAddress("A").createdAt(LocalDateTime.now()).build());
        tm.commit(status);

        // 2. 실행: 주문을 찾지 못하는 예외(ORDER_NOT_FOUND)가 발생하지 않아야 함
        orderService.notPaidOrder(o.getOrderId());

        // 3. 검증
        assertThat(orderRepository.findById(o.getOrderId())).isPresent();
    }
    
}
