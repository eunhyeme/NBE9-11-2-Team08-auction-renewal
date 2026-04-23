package com.team8.backend.domain.order.scheduler;

import com.team8.backend.domain.auction.entity.Auction;
import com.team8.backend.domain.auction.entity.AuctionStatus;
import com.team8.backend.domain.auction.repository.AuctionRepository;
import com.team8.backend.domain.order.dto.request.OrderTrackingRequest;
import com.team8.backend.domain.order.entity.Order;
import com.team8.backend.domain.order.entity.OrderStatus;
import com.team8.backend.domain.order.facade.OrderFacade;
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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
public class OrderSchedulerConcurrencyTest {

    @Autowired private OrderScheduler orderScheduler;
    @Autowired private OrderFacade orderFacade;
    @Autowired private OrderRepository orderRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private AuctionRepository auctionRepository;
    @Autowired private PlatformTransactionManager tm;

    private Long sellerId;
    private Long bidderId;

    @BeforeEach
    void setUp() {
        TransactionStatus deleteStatus = tm.getTransaction(new DefaultTransactionDefinition());
        orderRepository.deleteAllInBatch();
        auctionRepository.deleteAllInBatch();
        userRepository.deleteAllInBatch();
        tm.commit(deleteStatus);

        TransactionStatus createStatus = tm.getTransaction(new DefaultTransactionDefinition());
        User seller = userRepository.save(User.create("s_con@t.com", "p", "S", "서울"));
        User bidder = userRepository.save(User.create("b_con@t.com", "p", "B", "인천"));
        this.sellerId = seller.getUserId();
        this.bidderId = bidder.getUserId();
        tm.commit(createStatus);
    }

    @Test
    @DisplayName("결제 마감 스케줄러와 사용자의 결제가 동시에 발생하면 데이터 정합성이 유지된다")
    void concurrency_Payment_Vs_Scheduler() throws InterruptedException {
        TransactionStatus status = tm.getTransaction(new DefaultTransactionDefinition());
        User s = userRepository.findById(sellerId).orElseThrow();
        User b = userRepository.findById(bidderId).orElseThrow();
        Auction a = auctionRepository.save(Auction.builder()
                .seller(s).title("경매1").startPrice(100L).currentPrice(100L)
                .bidUnit(10).status(AuctionStatus.WON).startedAt(LocalDateTime.now()).endedAt(LocalDateTime.now()).build());
        Order o = orderRepository.save(Order.builder()
                .auction(a).bidder(b).seller(s).finalPrice(10000L).shippingAddress("서울")
                .createdAt(LocalDateTime.now().minusHours(49)).build());
        tm.commit(status);

        final Long orderId = o.getOrderId();
        CountDownLatch latch = new CountDownLatch(2);

        try (ExecutorService executorService = Executors.newFixedThreadPool(2)) {
            executorService.submit(() -> {
                try { orderScheduler.handleOrderTimeouts(); }
                catch (Exception ignored) {}
                finally { latch.countDown(); }
            });

            executorService.submit(() -> {
                try { orderFacade.payOrder(orderId, bidderId); }
                catch (Exception ignored) {}
                finally { latch.countDown(); }
            });

            latch.await();
        }

        Order result = orderRepository.findById(orderId).orElseThrow();
        assertThat(result.getStatus()).isIn(OrderStatus.NOT_PAID, OrderStatus.PAID);
    }

    @Test
    @DisplayName("발송 마감 스케줄러와 판매자의 발송이 동시에 발생하면 데이터 정합성이 유지된다")
    void concurrency_Shipping_Vs_Scheduler() throws InterruptedException {
        TransactionStatus status = tm.getTransaction(new DefaultTransactionDefinition());
        User s = userRepository.findById(sellerId).orElseThrow();
        User b = userRepository.findById(bidderId).orElseThrow();
        Auction a = auctionRepository.save(Auction.builder()
                .seller(s).title("경매2").startPrice(100L).currentPrice(100L)
                .bidUnit(10).status(AuctionStatus.WON).startedAt(LocalDateTime.now()).endedAt(LocalDateTime.now()).build());

        Order o = Order.builder()
                .auction(a).bidder(b).seller(s).finalPrice(1000L).shippingAddress("주소")
                .createdAt(LocalDateTime.now().minusHours(100)).build();
        o.pay(LocalDateTime.now().minusHours(73));
        o = orderRepository.save(o);
        tm.commit(status);

        final Long orderId = o.getOrderId();
        CountDownLatch latch = new CountDownLatch(2);

        // 💡 try-with-resources를 사용하여 리소스 자동 관리
        try (ExecutorService executorService = Executors.newFixedThreadPool(2)) {
            executorService.submit(() -> {
                try { orderScheduler.handleOrderTimeouts(); }
                catch (Exception ignored) {}
                finally { latch.countDown(); }
            });

            executorService.submit(() -> {
                try {
                    // 💡 인수 불일치 해결: "TRACK123" 하나만 전달
                    OrderTrackingRequest request = new OrderTrackingRequest("TRACK123");
                    orderFacade.registerTracking(orderId, request, sellerId);
                } catch (Exception ignored) {}
                finally { latch.countDown(); }
            });

            latch.await();
        }

        Order result = orderRepository.findById(orderId).orElseThrow();
        assertThat(result.getStatus()).isIn(OrderStatus.NOT_DELIVERED, OrderStatus.DELIVERING);
    }
}
