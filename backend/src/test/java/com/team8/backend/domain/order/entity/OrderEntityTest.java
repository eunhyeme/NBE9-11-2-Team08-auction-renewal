package com.team8.backend.domain.order.entity;

import com.team8.backend.domain.auction.entity.Auction;
import com.team8.backend.domain.auction.entity.AuctionStatus;
import com.team8.backend.domain.auction.repository.AuctionRepository;
import com.team8.backend.domain.order.repository.OrderRepository;
import com.team8.backend.domain.user.entity.User;
import com.team8.backend.domain.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class OrderEntityTest {

    @Autowired private OrderRepository orderRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private AuctionRepository auctionRepository;

    @Test
    @DisplayName("상태 변경 시 @Version 값이 증가한다")
    void versionIncrementTest() {
        // 1. Given: 실제 DB에 주문을 저장 (버전은 0 또는 1로 시작)
        Order order = orderRepository.save(createOrder());
        Long initialVersion = order.getVersion();

        // 2. When: 결제 처리 (상태 변경)
        order.pay(LocalDateTime.now());
        orderRepository.saveAndFlush(order);

        // 3. Then: 버전이 1 증가했는지 확인
        assertThat(order.getVersion()).isEqualTo(initialVersion + 1);
    }

    @Test
    @DisplayName("결제 시 상태가 PAID로 변경되고 72시간의 출고 기한이 설정된다")
    void pay_Success() {
        Order order = Order.builder().createdAt(LocalDateTime.now()).build();
        LocalDateTime paidAt = LocalDateTime.now();

        order.pay(paidAt);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.PAID);
        assertThat(order.getPaidAt()).isEqualTo(paidAt);
        assertThat(order.getDeliveryDeadline()).isEqualTo(paidAt.plusHours(72));
    }

    @Test
    @DisplayName("48시간 기한이 지난 후에만 markNotPaid가 상태를 변경한다")
    void markNotPaid_TimeCheck() {
        Order oldOrder = Order.builder().createdAt(LocalDateTime.now().minusHours(49)).build();
        oldOrder.markNotPaid();
        assertThat(oldOrder.getStatus()).isEqualTo(OrderStatus.NOT_PAID);

        Order newOrder = Order.builder().createdAt(LocalDateTime.now()).build();
        newOrder.markNotPaid();
        assertThat(newOrder.getStatus()).isEqualTo(OrderStatus.AWAITING_PAYMENT);
    }

    private Order createOrder() {
        User user = userRepository.save(User.create("test@test.com", "pw", "nick", "addr"));
        Auction auction = auctionRepository.save(Auction.builder()
                .seller(user).title("테스트경매").startPrice(100L).currentPrice(100L)
                .bidUnit(10).status(AuctionStatus.WON)
                .startedAt(LocalDateTime.now()).endedAt(LocalDateTime.now()).build());

        return Order.builder()
                .auction(auction).bidder(user).seller(user)
                .finalPrice(1000L).shippingAddress("서울시")
                .createdAt(LocalDateTime.now()).build();
    }
}
