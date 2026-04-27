package com.team8.backend.domain.auction.scheduler;

import com.team8.backend.domain.auction.entity.Auction;
import com.team8.backend.domain.auction.entity.AuctionStatus;
import com.team8.backend.domain.auction.facade.AuctionFacadeImpl;
import com.team8.backend.domain.auction.repository.AuctionRepository;
import com.team8.backend.domain.auction.service.AuctionService;
import com.team8.backend.domain.bid.dto.BidRequest;
import com.team8.backend.domain.bid.repository.BidRepository;
import com.team8.backend.domain.bid.service.BidService;
import com.team8.backend.domain.order.repository.OrderRepository;
import com.team8.backend.domain.user.entity.User;
import com.team8.backend.domain.user.entity.UserPermission;
import com.team8.backend.domain.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
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
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class AuctionSchedulerConcurrencyTest2 {

    @Autowired AuctionService auctionService;
    @Autowired BidService bidService;
    @Autowired AuctionRepository auctionRepository;
    @Autowired UserRepository userRepository;
    @Autowired BidRepository bidRepository;
    @Autowired OrderRepository orderRepository;
    @Autowired AuctionFacadeImpl auctionFacade;

    User seller;
    User bidder;

    @BeforeEach
    void setUp() {
        seller = userRepository.save(User.create("seller@scheduler-test.com", "pw", "판매자", "서울"));
        seller.grantPermission(UserPermission.SELLER);
        userRepository.save(seller);

        bidder = userRepository.save(User.create("bidder@scheduler-test.com", "pw", "입찰자", "부산"));
    }

    @AfterEach
    void tearDown() {
        bidRepository.deleteAll();
        orderRepository.deleteAll();
        auctionRepository.deleteAll();
        userRepository.deleteAll();
    }

    /**
     * [버그 재현] 입찰로 endedAt이 5분 연장된 경매에 closeAuction을 동시 호출할 때
     * LockModeType.OPTIMISTIC으로 인한 불필요한 충돌이 발생한다.
     *
     * 재현 시나리오:
     *   1. 경매 endedAt = 종료 3분 전 (5분 연장 대상)
     *   2. 스케줄러가 이 경매를 "만료 대상"으로 조회해 closeAuction 시작
     *   3. 동시에 입찰 발생 → extendAuctionIfNeeded로 endedAt +5분 연장, version 증가
     *   4. closeAuction이 OPTIMISTIC 락으로 읽었던 version과 불일치
     *      → OptimisticLockingFailureException 발생 → 3번 retry → 전부 실패 → 에러 로그
     *   5. (실제로는 경매가 연장되어 올바르게 ONGOING 상태이지만 에러 로그가 남음)
     *
     * 기대 동작:
     *   - 입찰로 endedAt이 연장된 경매는 closeAuction 후에도 ONGOING이어야 한다.
     *   - close() 내부 guard(endedAt 미래 체크)가 작동하므로 경매 상태는 정상이지만,
     *     findByIdWithLock의 LockModeType.OPTIMISTIC 때문에 불필요한 충돌이 발생한다.
     */
    @Test
    @DisplayName("[버그] 입찰로 5분 연장된 경매에 closeAuction이 동시에 실행되면 불필요한 충돌이 발생한다")
    void 입찰로_5분_연장된_경매에_closeAuction_동시_실행_시_경매는_ONGOING이어야_한다() throws InterruptedException {
        // endedAt = 3분 후 (5분 이내 → 입찰 시 자동 연장 대상)
        Auction auction = auctionRepository.save(Auction.builder()
                .seller(seller)
                .title("연장 테스트 경매")
                .description("설명")
                .imageUrl("test.jpg")
                .startPrice(10_000L)
                .currentPrice(10_000L)
                .bidUnit(1_000)
                .status(AuctionStatus.ONGOING)
                .startedAt(LocalDateTime.now().minusHours(1))
                .endedAt(LocalDateTime.now().plusMinutes(3))
                .build());

        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(2);
        AtomicInteger exceptionCount = new AtomicInteger(0);

        ExecutorService executor = Executors.newFixedThreadPool(2);

        // Thread A: 입찰 → extendAuctionIfNeeded → endedAt 연장, version 증가
        executor.submit(() -> {
            try {
                startLatch.await();
                bidService.createBid(bidder.getUserId(),
                        new BidRequest(auction.getAuctionId(), 11_000L));
            } catch (Exception e) {
                exceptionCount.incrementAndGet();
            } finally {
                doneLatch.countDown();
            }
        });

        // Thread B: 스케줄러 closeAuction 시뮬레이션
        // 입찰이 version을 증가시키면 OPTIMISTIC 락이 충돌 → retry → close() 내부 guard로 조기 리턴
        executor.submit(() -> {
            try {
                startLatch.await();
                auctionFacade.closeAuction(auction.getAuctionId());
            } catch (Exception e) {
                // closeAuction은 REQUIRES_NEW 트랜잭션이므로 충돌 시 예외가 밖으로 전파되지 않아야 한다
                // 스케줄러가 catch해서 retry하기 때문 → 여기에 도달하면 안 됨
                exceptionCount.incrementAndGet();
            } finally {
                doneLatch.countDown();
            }
        });

        startLatch.countDown();
        doneLatch.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        // 경매는 입찰로 endedAt이 연장되었으므로 ONGOING이어야 한다
        Auction result = auctionRepository.findById(auction.getAuctionId()).orElseThrow();
        assertThat(result.getStatus())
                .as("5분 연장된 경매는 closeAuction 후에도 ONGOING이어야 합니다.")
                .isEqualTo(AuctionStatus.ONGOING);

        // closeAuction이 예외 없이 완료되어야 한다 (내부적으로 retry하더라도)
        assertThat(exceptionCount.get())
                .as("closeAuction은 OptimisticLock 충돌을 내부에서 처리해야 합니다.")
                .isZero();
    }

    /**
     * [정상 케이스] closeAuction이 동시에 두 번 호출되어도 주문은 하나만 생성되어야 한다.
     *
     * 보호 메커니즘:
     *   1. closeAuction 내부: status != ONGOING 체크 (guard)
     *   2. @Version 낙관적 락: 동시 UPDATE 시 하나만 성공
     *   3. auction.close() 내부: status/endedAt 이중 체크
     *
     * 이 테스트가 실패하면 중복 주문이 생성되는 심각한 버그를 의미한다.
     */
    @Test
    @DisplayName("[정상] closeAuction 동시 호출 시 주문은 하나만 생성되어야 한다")
    void closeAuction_동시_호출_시_주문은_하나만_생성되어야_한다() throws InterruptedException {
        // 낙찰자가 있는 만료된 경매
        Auction auction = auctionRepository.save(Auction.builder()
                .seller(seller)
                .title("낙찰 경매")
                .description("설명")
                .imageUrl("test.jpg")
                .startPrice(10_000L)
                .currentPrice(50_000L)
                .lastBidder(bidder)
                .bidUnit(1_000)
                .status(AuctionStatus.ONGOING)
                .startedAt(LocalDateTime.now().minusDays(2))
                .endedAt(LocalDateTime.now().minusMinutes(1))
                .build());

        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(2);

        ExecutorService executor = Executors.newFixedThreadPool(2);

        Runnable closeTask = () -> {
            try {
                startLatch.await();
                auctionFacade.closeAuction(auction.getAuctionId());
            } catch (Exception ignored) {
                // OptimisticLock 충돌은 스케줄러가 처리 (retry)
            } finally {
                doneLatch.countDown();
            }
        };

        executor.submit(closeTask);
        executor.submit(closeTask);

        startLatch.countDown();
        doneLatch.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        // 중복 주문 생성 방지 검증
        long orderCount = orderRepository.count();
        assertThat(orderCount)
                .as("closeAuction이 두 번 호출되어도 주문은 정확히 하나만 생성되어야 합니다.")
                .isEqualTo(1L);

        // 경매 상태 검증
        Auction result = auctionRepository.findById(auction.getAuctionId()).orElseThrow();
        assertThat(result.getStatus())
                .as("낙찰자가 있는 경매는 WON 상태가 되어야 합니다.")
                .isEqualTo(AuctionStatus.WON);
    }

    /**
     * [정상 케이스] 낙찰자 없는 경매는 closeAuction 시 UNSOLD가 되어야 하고 주문이 생성되지 않아야 한다.
     */
    @Test
    @DisplayName("[정상] 입찰자 없는 경매는 closeAuction 시 UNSOLD가 되고 주문이 생성되지 않아야 한다")
    void 입찰자_없는_경매는_closeAuction_시_UNSOLD가_되고_주문이_생성되지_않아야_한다() {
        Auction auction = auctionRepository.save(Auction.builder()
                .seller(seller)
                .title("유찰 경매")
                .description("설명")
                .imageUrl("test.jpg")
                .startPrice(10_000L)
                .currentPrice(10_000L)
                .bidUnit(1_000)
                .status(AuctionStatus.ONGOING)
                .startedAt(LocalDateTime.now().minusDays(2))
                .endedAt(LocalDateTime.now().minusMinutes(1))
                .build());

        auctionFacade.closeAuction(auction.getAuctionId());

        Auction result = auctionRepository.findById(auction.getAuctionId()).orElseThrow();
        assertThat(result.getStatus()).isEqualTo(AuctionStatus.UNSOLD);
        assertThat(orderRepository.count()).isZero();
    }

    /**
     * [정상 케이스] 이미 종료된 경매에 closeAuction을 다시 호출해도 상태가 바뀌지 않아야 한다.
     * (ONGOING이 아닌 상태에서 closeAuction 호출 → 조기 리턴)
     */
    @Test
    @DisplayName("[정상] 이미 WON 상태인 경매에 closeAuction 재호출 시 상태가 바뀌지 않아야 한다")
    void 이미_WON된_경매에_closeAuction_재호출_시_상태가_변경되지_않아야_한다() {
        Auction auction = auctionRepository.save(Auction.builder()
                .seller(seller)
                .title("낙찰된 경매")
                .description("설명")
                .imageUrl("test.jpg")
                .startPrice(10_000L)
                .currentPrice(50_000L)
                .lastBidder(bidder)
                .bidUnit(1_000)
                .status(AuctionStatus.ONGOING)
                .startedAt(LocalDateTime.now().minusDays(2))
                .endedAt(LocalDateTime.now().minusMinutes(1))
                .build());

        // 1차 closeAuction → WON + 주문 생성
        auctionFacade.closeAuction(auction.getAuctionId());
        assertThat(orderRepository.count()).isEqualTo(1L);

        // 2차 closeAuction → status != ONGOING → 조기 리턴, 주문 추가 생성 없음
        auctionFacade.closeAuction(auction.getAuctionId());

        assertThat(orderRepository.count())
                .as("이미 종료된 경매에 closeAuction 재호출 시 추가 주문이 생성되어서는 안 됩니다.")
                .isEqualTo(1L);

        Auction result = auctionRepository.findById(auction.getAuctionId()).orElseThrow();
        assertThat(result.getStatus()).isEqualTo(AuctionStatus.WON);
    }
}
