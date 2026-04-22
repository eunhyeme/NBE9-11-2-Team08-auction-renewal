package com.team8.backend.domain.order.service;

import com.team8.backend.domain.auction.entity.Auction;
import com.team8.backend.domain.auction.entity.AuctionStatus;
import com.team8.backend.domain.auction.repository.AuctionRepository;
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

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class OrderServiceIntegrationTest {

    @Autowired private OrderService orderService;
    @Autowired private OrderRepository orderRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private AuctionRepository auctionRepository;

    private User seller;
    private User bidder;
    private Auction auction;

    @BeforeEach
    void setUp() {
        orderRepository.deleteAll();
        auctionRepository.deleteAll();
        userRepository.deleteAll();

        seller = userRepository.save(User.create("s1@t.com", "pw", "판매자", "주소"));
        bidder = userRepository.save(User.create("b1@t.com", "pw", "낙찰자", "주소"));
        auction = auctionRepository.save(Auction.builder()
                .seller(seller).title("상품").startPrice(1000L).currentPrice(10000L)
                .bidUnit(100).status(AuctionStatus.WON)
                .startedAt(LocalDateTime.now().minusDays(1)).endedAt(LocalDateTime.now())
                .lastBidder(bidder).build());
    }

    @Test
    @DisplayName("주문 생성 및 배송지 수정이 정상 동작한다")
    void createAndUpdateOrder() {
        orderService.createOrder(auction, bidder, 10000L);
        Order order = orderRepository.findAll().get(0);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.AWAITING_PAYMENT);

        orderService.updateAddress(order, "변경 주소");
        assertThat(order.getShippingAddress()).isEqualTo("변경 주소");
    }

    @Test
    @DisplayName("결제부터 배송 시작까지 전체 흐름을 검증한다")
    void fullOrderFlow() {
        orderService.createOrder(auction, bidder, 10000L);
        Order order = orderRepository.findAll().get(0);

        orderService.processPayment(order);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PAID);

        orderService.processShipping(order, "TRK123");
        assertThat(order.getStatus()).isEqualTo(OrderStatus.DELIVERING);
        assertThat(order.getTrackingNumber()).isEqualTo("TRK123");
    }

    @Test
    @DisplayName("필터(진행중/완료)에 따른 내 거래 내역 조회가 정상 동작한다")
    void getMyOrders_WithFilters() {
        orderService.createOrder(auction, bidder, 10000L);

        var ongoingOrders = orderService.getMyOrders(bidder.getUserId(), OrderHistoryRole.BUYER, OrderFilter.ONGOING);
        assertThat(ongoingOrders).hasSize(1);

        var completedOrders = orderService.getMyOrders(bidder.getUserId(), OrderHistoryRole.BUYER, OrderFilter.COMPLETED);
        assertThat(completedOrders).isEmpty();
    }
}
