package com.team8.backend.domain.order.facade;

import com.team8.backend.domain.auction.entity.Auction;
import com.team8.backend.domain.auction.entity.AuctionStatus;
import com.team8.backend.domain.auction.repository.AuctionRepository;
import com.team8.backend.domain.order.entity.Order;
import com.team8.backend.domain.order.repository.OrderRepository;
import com.team8.backend.domain.user.entity.User;
import com.team8.backend.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
public class OrderConcurrencyTest {

    @Autowired private OrderFacade orderFacade;
    @Autowired private OrderRepository orderRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private AuctionRepository auctionRepository;

    private Long orderId;
    private Long userId;

    @BeforeEach
    void setUp() {
        orderRepository.deleteAll();
        auctionRepository.deleteAll();
        userRepository.deleteAll();
        User s = userRepository.save(User.create("s_c@t.com", "p", "S", "A"));
        User b = userRepository.save(User.create("b_c@t.com", "p", "B", "A"));
        userId = b.getUserId();
        // currentPrice 및 필수 시간/상태 필드 추가
        Auction a = auctionRepository.save(Auction.builder()
                .seller(s).title("T").startPrice(100L).currentPrice(100L).bidUnit(10)
                .status(AuctionStatus.WON).startedAt(LocalDateTime.now()).endedAt(LocalDateTime.now()).build());
        Order o = orderRepository.save(Order.builder().auction(a).bidder(b).seller(s)
                .finalPrice(1000L).shippingAddress("A").createdAt(LocalDateTime.now()).build());
        orderId = o.getOrderId();
    }

    @Test
    @DisplayName("동시에 10번 결제 요청 시 1번만 성공한다")
    void concurrentPayment() throws InterruptedException {
        int threads = 10;
        ExecutorService es = Executors.newFixedThreadPool(threads);
        CountDownLatch latch = new CountDownLatch(threads);
        AtomicInteger success = new AtomicInteger();

        for (int i = 0; i < threads; i++) {
            es.execute(() -> {
                try {
                    orderFacade.payOrder(orderId, userId);
                    success.incrementAndGet();
                } catch (Exception ignored) {}
                finally { latch.countDown(); }
            });
        }
        latch.await();
        es.shutdown();

        assertThat(success.get()).isEqualTo(1);
    }
}
