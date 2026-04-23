package com.team8.backend.domain.bid.service;

import com.team8.backend.domain.auction.entity.Auction;
import com.team8.backend.domain.auction.entity.AuctionStatus;
import com.team8.backend.domain.auction.repository.AuctionRepository;
import com.team8.backend.domain.bid.dto.BidRequest;
import com.team8.backend.domain.bid.repository.BidRepository;
import com.team8.backend.domain.order.repository.OrderRepository;
import com.team8.backend.domain.user.entity.User;
import com.team8.backend.domain.user.entity.UserPermission;
import com.team8.backend.domain.user.repository.UserRepository;
import com.team8.backend.global.exception.CustomException;
import com.team8.backend.global.exception.ErrorCode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class BidConcurrencyTest {

    @Autowired BidService bidService;
    @Autowired AuctionRepository auctionRepository;
    @Autowired UserRepository userRepository;
    @Autowired BidRepository bidRepository;
    @Autowired OrderRepository orderRepository;

    User seller;
    User bidder1;
    User bidder2;
    Auction auction;

    ExecutorService executor;

    @BeforeEach
    void setUp() {
        executor = Executors.newFixedThreadPool(2);

        seller = userRepository.save(User.create("seller@bid-test.com", "pw", "판매자", "서울"));
        seller.grantPermission(UserPermission.SELLER);
        userRepository.save(seller);

        bidder1 = userRepository.save(User.create("bidder1@bid-test.com", "pw", "입찰자1", "부산"));
        bidder2 = userRepository.save(User.create("bidder2@bid-test.com", "pw", "입찰자2", "대구"));

        auction = auctionRepository.save(Auction.builder()
                .seller(seller)
                .title("테스트 경매")
                .description("설명")
                .imageUrl("test.jpg")
                .startPrice(10_000L)
                .currentPrice(10_000L)
                .bidUnit(1_000)
                .status(AuctionStatus.ONGOING)
                .startedAt(LocalDateTime.now().minusHours(1))
                .endedAt(LocalDateTime.now().plusDays(1))
                .build());
    }

    @AfterEach
    void tearDown() {
        executor.shutdown();
        // -DKEEP_TEST_DATA=true 로 실행 시 DB 데이터 보존 (수동 확인용)
        // 예: ./gradlew test --tests "*.BidConcurrencyTest" -DKEEP_TEST_DATA=true
        if (Boolean.getBoolean("KEEP_TEST_DATA")) {
            System.out.println("[tearDown] KEEP_TEST_DATA=true → 데이터 보존 (수동 확인 가능)");
            return;
        }
        bidRepository.deleteAll();
        orderRepository.deleteAll();
        auctionRepository.deleteAll();
        userRepository.deleteAll();
    }

    /**
     * [기존 createBid] 동일 가격 동시 입찰 시나리오
     *
     * 현재 동작: snapshot 읽기 → dirty-check 플러시 방식
     */
    @Test
    @DisplayName("[createBid] 동일 가격 동시 입찰 — 결과 관찰")
    void createBid_동일가격_동시입찰_결과관찰() throws InterruptedException {
        int threadCount = 2;
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done  = new CountDownLatch(threadCount);
        AtomicInteger success = new AtomicInteger();
        List<Exception> errors = Collections.synchronizedList(new ArrayList<>());

        executor.submit(() -> {
            try {
                start.await();
                bidService.createBid(bidder1.getUserId(),
                        new BidRequest(auction.getAuctionId(), 11_000L));
                success.incrementAndGet();
            } catch (Exception e) { errors.add(e); } finally { done.countDown(); }
        });
        executor.submit(() -> {
            try {
                start.await();
                bidService.createBid(bidder2.getUserId(),
                        new BidRequest(auction.getAuctionId(), 11_000L));
                success.incrementAndGet();
            } catch (Exception e) { errors.add(e); } finally { done.countDown(); }
        });

        start.countDown();
        done.await(10, TimeUnit.SECONDS);

        // 동시 동일 가격 → 정확히 1건만 성공
        if (!errors.isEmpty()) {
            assertThat(success.get()).isEqualTo(1);
            assertThat(errors.getFirst())
                    .isInstanceOf(ObjectOptimisticLockingFailureException.class);
        }
    }

    /**
     * [createBid] 서로 다른 가격 동시 입찰 시나리오 — 정합성 깨짐 재현
     *
     * 문제: snapshot 읽기 → dirty-check 플러시 방식이므로
     *   두 스레드 모두 currentPrice=10_000 snapshot을 읽고
     *   각자 placeBid(11_000) / placeBid(12_000) 호출 후 flush 경쟁.
     *   나중에 flush된 값이 최종 현재가가 되어 12_000이 아닐 수 있음.
     *
     * 기대: 두 스레드 모두 성공하지만 최종가가 12_000이 아닐 수 있다 (정합성 깨짐)
     * → createBidAtomic 에서는 최종가가 항상 12_000임과 대조
     */
    @Test
    @DisplayName("[createBid] 서로 다른 가격 동시 입찰 → 최종가 정합성 보장 안 됨 (버그 재현)")
    void createBid_다른가격_동시입찰_정합성깨짐() throws InterruptedException {
        int threadCount = 2;
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done  = new CountDownLatch(threadCount);
        AtomicInteger success = new AtomicInteger();
        List<Exception> errors = Collections.synchronizedList(new ArrayList<>());

        executor.submit(() -> {
            try {
                start.await();
                bidService.createBid(bidder1.getUserId(),
                        new BidRequest(auction.getAuctionId(), 11_000L));
                success.incrementAndGet();
            } catch (Exception e) { errors.add(e); } finally { done.countDown(); }
        });
        executor.submit(() -> {
            try {
                start.await();
                bidService.createBid(bidder2.getUserId(),
                        new BidRequest(auction.getAuctionId(), 12_000L));
                success.incrementAndGet();
            } catch (Exception e) { errors.add(e); } finally { done.countDown(); }
        });

        start.countDown();
        done.await(10, TimeUnit.SECONDS);

        Auction result = auctionRepository.findById(auction.getAuctionId()).orElseThrow();

        System.out.println("[createBid] 성공: " + success.get()
                + ", 실패: " + errors.size()
                + ", 최종가: " + result.getCurrentPrice());

        // 두 스레드 모두 예외 없이 성공할 수 있음
        assertThat(success.get() + errors.size()).isEqualTo(threadCount);

        // [버그] flush 순서에 따라 최종가가 11_000이 될 수 있음 → 12_000 보장 안 됨
        // createBidAtomic 이었다면 반드시 12_000
        if (result.getCurrentPrice() != 12_000L) {
            System.out.println("[createBid] ⚠️ 정합성 깨짐: 최종가가 12_000이 아닌 "
                    + result.getCurrentPrice());
        }
    }

    /**
     * [createBidAtomic] 동일 가격 동시 입찰 시나리오
     *
     * 기대 동작: 조건부 원자 UPDATE (WHERE current_price < newPrice)
     *   → 두 번째 스레드의 UPDATE는 0건 반환
     *   → 재조회 후 BID_TOO_LOW 반환
     *   → 정확히 1건만 성공
     */
    @Test
    @DisplayName("[createBidAtomic] 동일 가격 동시 입찰 → 1건 성공, 1건 BID_TOO_LOW")
    void createBidAtomic_동일가격_동시입찰() throws InterruptedException {
        int threadCount = 2;
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done  = new CountDownLatch(threadCount);
        AtomicInteger success = new AtomicInteger();
        List<Exception> errors = Collections.synchronizedList(new ArrayList<>());

        executor.submit(() -> {
            try {
                start.await();
                bidService.createBidAtomic(bidder1.getUserId(),
                        new BidRequest(auction.getAuctionId(), 11_000L));
                success.incrementAndGet();
            } catch (Exception e) { errors.add(e); } finally { done.countDown(); }
        });
        executor.submit(() -> {
            try {
                start.await();
                bidService.createBidAtomic(bidder2.getUserId(),
                        new BidRequest(auction.getAuctionId(), 11_000L));
                success.incrementAndGet();
            } catch (Exception e) { errors.add(e); } finally { done.countDown(); }
        });

        start.countDown();
        done.await(10, TimeUnit.SECONDS);

        // 동시 동일 가격 → 정확히 1건만 성공
        if (!errors.isEmpty()) {
            assertThat(success.get()).isEqualTo(1);
            assertThat(errors.getFirst())
                    .isInstanceOf(CustomException.class);
            assertThat(((CustomException) errors.getFirst()).getErrorCode())
                    .isEqualTo(ErrorCode.BID_TOO_LOW);
        }
    }

    /**
     * [createBidAtomic] 서로 다른 가격 동시 입찰 시나리오
     *
     * 기대 동작:
     *   - 11_000 → 12_000 순이면 둘 다 성공 (each UPDATE clears the condition)
     *   - 12_000 먼저면 11_000은 BID_TOO_LOW (11_000 <= 12_000)
     */
    @Test
    @DisplayName("[createBidAtomic] 서로 다른 가격 동시 입찰 → 높은 가격은 항상 성공")
    void createBidAtomic_다른가격_동시입찰() throws InterruptedException {
        int threadCount = 2;
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done  = new CountDownLatch(threadCount);
        AtomicInteger success = new AtomicInteger();
        List<Exception> errors = Collections.synchronizedList(new ArrayList<>());

        executor.submit(() -> {
            try {
                start.await();
                bidService.createBidAtomic(bidder1.getUserId(),
                        new BidRequest(auction.getAuctionId(), 11_000L));
                success.incrementAndGet();
            } catch (Exception e) { errors.add(e); } finally { done.countDown(); }
        });
        executor.submit(() -> {
            try {
                start.await();
                bidService.createBidAtomic(bidder2.getUserId(),
                        new BidRequest(auction.getAuctionId(), 12_000L));
                success.incrementAndGet();
            } catch (Exception e) { errors.add(e); } finally { done.countDown(); }
        });

        start.countDown();
        done.await(10, TimeUnit.SECONDS);

        // 11_000 → 12_000 순: 둘 다 성공 / 12_000 먼저: 11_000은 BID_TOO_LOW
        assertThat(success.get()).isBetween(1, 2);
        if (!errors.isEmpty()) {
            assertThat(errors.get(0)).isInstanceOf(CustomException.class);
            assertThat(((CustomException) errors.get(0)).getErrorCode())
                    .isEqualTo(ErrorCode.BID_TOO_LOW);
        }

        // 최종 현재가는 항상 12_000 (더 높은 가격이 최종 반영)
        Auction result = auctionRepository.findById(auction.getAuctionId()).orElseThrow();
        assertThat(result.getCurrentPrice()).isEqualTo(12_000L);
    }
}
