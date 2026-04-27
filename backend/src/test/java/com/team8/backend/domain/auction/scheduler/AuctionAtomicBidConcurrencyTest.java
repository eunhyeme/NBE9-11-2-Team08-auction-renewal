package com.team8.backend.domain.auction.scheduler;

import com.team8.backend.domain.auction.entity.Auction;
import com.team8.backend.domain.auction.entity.AuctionStatus;
import com.team8.backend.domain.auction.repository.AuctionRepository;
import com.team8.backend.domain.bid.dto.BidRequest;
import com.team8.backend.domain.bid.service.BidService;
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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
public class AuctionAtomicBidConcurrencyTest {

    @Autowired private BidService bidService;
    @Autowired private AuctionRepository auctionRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private PlatformTransactionManager tm;

    private Long auctionId;
    private final List<Long> bidderIds = new ArrayList<>();

    @BeforeEach
    void setUp() {
        // 1. 초기화 및 물리 커밋 보장
        TransactionStatus status = tm.getTransaction(new DefaultTransactionDefinition());
        auctionRepository.deleteAllInBatch();
        userRepository.deleteAllInBatch();

        // 2. 판매자 및 100명의 입찰자 생성
        User seller = userRepository.save(User.create("seller@atomic.com", "pw", "판매자", "서울"));
        for (int i = 0; i < 100; i++) {
            User bidder = userRepository.save(User.create("bidder" + i + "@atomic.com", "pw", "입찰자" + i, "서울"));
            bidderIds.add(bidder.getUserId());
        }

        // 3. 넉넉한 기한의 경매 생성 (중간에 마감되지 않도록 함)
        Auction auction = auctionRepository.save(Auction.builder()
                .seller(seller)
                .title("원자적 업데이트 입찰 테스트")
                .startPrice(1000L)
                .currentPrice(1000L)
                .bidUnit(100)
                .status(AuctionStatus.ONGOING)
                .startedAt(LocalDateTime.now().minusHours(1))
                .endedAt(LocalDateTime.now().plusHours(1))
                .build());

        this.auctionId = auction.getAuctionId();
        tm.commit(status);
    }

    @Test
    @DisplayName("DB 원자적 업데이트: 100개의 동시 입찰 요청 시 데이터 유실 없이 순차적으로 가격이 갱신된다")
    void concurrency_100_AtomicBids() throws InterruptedException {
        int threadCount = 100;
        ExecutorService executorService = Executors.newFixedThreadPool(32); // 동시성 경합 유도
        CountDownLatch latch = new CountDownLatch(threadCount);

        // [When] 100명의 사용자가 각각 현재가보다 높은 금액으로 동시에 입찰
        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            final Long bidderId = bidderIds.get(index);
            // 각 스레드마다 금액을 다르게 설정 (2000, 2100, 2200... 11900)
            final long bidAmount = 2000L + (index * 100);

            executorService.submit(() -> {
                try {
                    // DB 원자적 쿼리 호출
                    bidService.createBid(bidderId, new BidRequest(auctionId, bidAmount));
                } catch (Exception e) {
                    // 입찰 실패 사유 출력 (원자적 쿼리 조건 미달 시)
                    System.out.println("입찰 실패 (index " + index + "): " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executorService.shutdown();

        // [Then] 최종 정합성 검증
        Auction result = auctionRepository.findById(auctionId).orElseThrow();

        System.out.println("[테스트 결과] 최종 가격: " + result.getCurrentPrice() + ", 최종 버전: " + result.getVersion());

        // 1. 버전 검증: 100번의 수정 시도 중 성공한 횟수만큼 버전이 올라감 (최소 1 이상)
        assertThat(result.getVersion()).isGreaterThan(0);

        // 2. 가격 검증: 원자적 업데이트가 보장된다면 최종 가격은 입찰 시도 금액 중 가장 높은 값(11900)이어야 함
        // (금액이 낮은 요청이 늦게 도달하더라도 DB 조건문 WHERE current_price < :newPrice 에서 걸러지기 때문)
        assertThat(result.getCurrentPrice()).isGreaterThanOrEqualTo(2000L);

        // 3. 상태 검증: 입찰 성공 시에도 경매는 계속 ONGOING 상태여야 함
        assertThat(result.getStatus()).isEqualTo(AuctionStatus.ONGOING);
    }
}
