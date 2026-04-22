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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class BidConcurrencyTest {
//
//    @Autowired
//    BidService bidService;
//    @Autowired
//    AuctionRepository auctionRepository;
//    @Autowired
//    UserRepository userRepository;
//    @Autowired
//    BidRepository bidRepository;
//    @Autowired
//    OrderRepository orderRepository;
//
//    User seller;
//    User bidder1;
//    User bidder2;
//    Auction auction;
//
//    @BeforeEach
//    void setUp() {
//        seller = userRepository.save(User.create("seller@bid-test.com", "pw", "판매자", "서울"));
//        seller.grantPermission(UserPermission.SELLER);
//        userRepository.save(seller);
//
//        bidder1 = userRepository.save(User.create("bidder1@bid-test.com", "pw", "입찰자1", "부산"));
//        bidder2 = userRepository.save(User.create("bidder2@bid-test.com", "pw", "입찰자2", "대구"));
//
//        auction = auctionRepository.save(Auction.builder()
//                .seller(seller)
//                .title("테스트 경매")
//                .description("설명")
//                .imageUrl("test.jpg")
//                .startPrice(10_000L)
//                .currentPrice(10_000L)
//                .bidUnit(1_000)
//                .status(AuctionStatus.ONGOING)
//                .startedAt(LocalDateTime.now().minusHours(1))
//                .endedAt(LocalDateTime.now().plusDays(1))
//                .build());
//    }
//
//    @AfterEach
//    void tearDown() {
//        bidRepository.deleteAll();
//        orderRepository.deleteAll();
//        auctionRepository.deleteAll();
//        userRepository.deleteAll();
//    }

//    /**
//     * [버그 재현] 두 사용자가 동시에 입찰할 때 OptimisticLockingFailureException이 발생한다.
//     *
//     * 현재 동작:
//     *   - createBid의 try-catch는 메서드 내부 예외만 잡는다.
//     *   - @Transactional 커밋 시점의 OptimisticLockingFailureException은 try-catch 밖에서 발생해
//     *     CustomException으로 변환되지 않고 그대로 노출된다.
//     *
//     * 기대 동작:
//     *   - 충돌한 입찰 요청은 CustomException(CONCURRENT_BID, 409)을 반환해야 한다.
//     */
//    @Test
//    @DisplayName("[버그] 동시 입찰 충돌 시 CONCURRENT_BID 에러가 반환되어야 한다")
//    void 동시_입찰_충돌_시_CONCURRENT_BID_에러가_반환되어야_한다() throws InterruptedException {
//        int threadCount = 2;
//        CountDownLatch startLatch = new CountDownLatch(1);
//        CountDownLatch doneLatch = new CountDownLatch(threadCount);
//        List<Exception> exceptions = Collections.synchronizedList(new ArrayList<>());
//        AtomicInteger successCount = new AtomicInteger(0);
//
//        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
//
//        // 같은 가격으로 동시 입찰 → 한 쪽이 커밋하면 다른 쪽은 반드시 실패
//        executor.submit(() -> {
//            try {
//                startLatch.await();
//                bidService.createBid(bidder1.getUserId(),
//                        new BidRequest(auction.getAuctionId(), 11_000L));
//                successCount.incrementAndGet();
//            } catch (Exception e) {
//                exceptions.add(e);
//            } finally {
//                doneLatch.countDown();
//            }
//        });
//
//        executor.submit(() -> {
//            try {
//                startLatch.await();
//                bidService.createBid(bidder2.getUserId(),
//                        new BidRequest(auction.getAuctionId(), 11_000L));
//                successCount.incrementAndGet();
//            } catch (Exception e) {
//                exceptions.add(e);
//            } finally {
//                doneLatch.countDown();
//            }
//        });
//
//        startLatch.countDown();
//        doneLatch.await(10, TimeUnit.SECONDS);
//        executor.shutdown();
//
//        // 동시 입찰이 충돌했다면 반드시 하나만 성공해야 한다
//        // (두 스레드 모두 성공했다면 충돌이 발생하지 않은 것 → 테스트를 재실행하면 충돌 확인 가능)
//        if (!exceptions.isEmpty()) {
//            assertThat(successCount.get()).isEqualTo(1);
//
//            // [현재 실패] OptimisticLockingFailureException이 그대로 노출됨
//            // [수정 후 통과] CustomException(CONCURRENT_BID)을 반환해야 함
//            assertThat(exceptions.get(0))
//                    .as("동시 입찰 충돌 시 CustomException(CONCURRENT_BID)을 반환해야 합니다. " +
//                            "현재: %s", exceptions.get(0).getClass().getSimpleName())
//                    .isInstanceOf(CustomException.class);
//
//            assertThat(((CustomException) exceptions.get(0)).getErrorCode())
//                    .isEqualTo(ErrorCode.CONCURRENT_BID);
//        }
//    }
}
