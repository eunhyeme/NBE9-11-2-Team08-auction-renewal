package com.team8.backend.domain.order.service;

import com.team8.backend.domain.auction.entity.Auction;
import com.team8.backend.domain.auction.entity.AuctionStatus;
import com.team8.backend.domain.auction.repository.AuctionRepository;
import com.team8.backend.domain.order.dto.response.OrderResponse;
import com.team8.backend.domain.order.entity.Order;
import com.team8.backend.domain.order.entity.OrderFilter;
import com.team8.backend.domain.order.entity.OrderHistoryRole;
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
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class OrderQueryIntegrationTest {

    @Autowired private OrderService orderService;
    @Autowired private OrderRepository orderRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private AuctionRepository auctionRepository;

    private User buyer;
    private User seller;

    @BeforeEach
    void setUp() {
        orderRepository.deleteAll();
        auctionRepository.deleteAll();
        userRepository.deleteAll();
        seller = userRepository.save(User.create("s_q@t.com", "p", "S", "J"));
        buyer = userRepository.save(User.create("b_q@t.com", "p", "B", "J"));

        // 진행중 2건, 완료 1건 생성
        createOrder(OrderStatus.AWAITING_PAYMENT, 0);
        createOrder(OrderStatus.PAID, 0);
        Order failed = createOrder(OrderStatus.AWAITING_PAYMENT, 50);
        failed.markNotPaid();
        orderRepository.save(failed);
    }

    @Test
    @DisplayName("조회 필터링 테스트: 진행 중 2건, 완료 1건 확인")
    void getMyOrders_Filter_Success() {
        // 구매자 진행 중 내역
        List<OrderResponse> ongoing = orderService.getMyOrders(buyer.getUserId(), OrderHistoryRole.BUYER, OrderFilter.ONGOING);
        assertThat(ongoing).hasSize(2);

        // 판매자 완료 내역
        List<OrderResponse> completed = orderService.getMyOrders(seller.getUserId(), OrderHistoryRole.SELLER, OrderFilter.COMPLETED);
        assertThat(completed).hasSize(1);
        // Record 접근자 status() 사용
        assertThat(completed.get(0).status()).isEqualTo(OrderStatus.NOT_PAID);
    }

    private Order createOrder(OrderStatus status, int hoursAgo) {
        Auction a = auctionRepository.save(Auction.builder()
                .seller(seller).title("T").startPrice(100L).currentPrice(100L)
                .bidUnit(10).status(AuctionStatus.WON).startedAt(LocalDateTime.now()).endedAt(LocalDateTime.now()).build());
        Order o = Order.builder().auction(a).bidder(buyer).seller(seller).finalPrice(1000L)
                .shippingAddress("Add").createdAt(LocalDateTime.now().minusHours(hoursAgo)).build();
        if (status == OrderStatus.PAID) o.pay(LocalDateTime.now());
        return orderRepository.save(o);
    }
}
