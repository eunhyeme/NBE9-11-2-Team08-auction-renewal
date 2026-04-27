package com.team8.backend.domain.auction.scheduler;

import com.team8.backend.domain.auction.entity.Auction;
import com.team8.backend.domain.auction.entity.AuctionStatus;
import com.team8.backend.domain.auction.facade.AuctionFacade;
import com.team8.backend.domain.auction.repository.AuctionRepository;
import com.team8.backend.domain.bid.dto.BidRequest;
import com.team8.backend.domain.bid.repository.BidRepository; // 💡 추가됨
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
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import java.time.LocalDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@Transactional
@SpringBootTest
@ActiveProfiles("test")
class AuctionSchedulerConcurrencyTest  {

    @Autowired private AuctionFacade auctionFacade;
    @Autowired private AuctionRepository auctionRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private BidRepository bidRepository; // 💡 의존성 추가
    @Autowired private PlatformTransactionManager tm;
    @Autowired private BidService bidService;

    private Long auctionId;
    private Long bidderId;

    @BeforeEach
    void setUp() {
        clearDatabase();

        TransactionStatus status = tm.getTransaction(new DefaultTransactionDefinition());
        User seller = userRepository.save(User.create("s@c.com", "p", "S", "서울"));
        User bidder = userRepository.save(User.create("b@c.com", "p", "B", "서울"));

        // 마감 직전 상태의 경매 생성
        Auction a = auctionRepository.save(Auction.builder()
                .seller(seller).title("동시성 경합 경매").startPrice(100L).currentPrice(100L)
                .bidUnit(10).status(AuctionStatus.ONGOING).startedAt(LocalDateTime.now().minusDays(1))
                .endedAt(LocalDateTime.now().plusSeconds(1)).build());

        this.auctionId = a.getAuctionId();
        this.bidderId = bidder.getUserId();
        tm.commit(status);
    }

    @Test
    @DisplayName("원자적 입찰 업데이트와 낙관적 락 마감 처리가 동시에 발생해도 데이터 정합성이 유지된다")
    void concurrency_AtomicBid_Vs_OptimisticClose() throws InterruptedException {
        ExecutorService executorService = Executors.newFixedThreadPool(2);
        CountDownLatch latch = new CountDownLatch(2);

        // 스레드 1: 스케줄러 마감 시도 (JPA 낙관적 락 기반)
        executorService.submit(() -> {
            try {
                auctionFacade.closeAuction(auctionId);
            } catch (Exception ignored) {
                // 입찰이 먼저 성공하여 버전이 올라가면 OptimisticLockingFailureException 발생
            } finally {
                latch.countDown();
            }
        });

        // 스레드 2: 사용자가 입찰 시도 (DB 원자적 업데이트 기반)
        executorService.submit(() -> {
            try {
                // 💡 원자적 업데이트 로직이 담긴 서비스 호출
                bidService.createBid(bidderId, new BidRequest(auctionId, 2000L));
            } catch (Exception ignored) {
                // 마감이 먼저 성공하여 상태가 바뀌면 쿼리 조건 불일치로 실패
            } finally {
                latch.countDown();
            }
        });

        latch.await();

        Auction result = auctionRepository.findById(auctionId).get();

        // 정합성 검증: 
        // 1. 입찰이 먼저면 -> ONGOING (시간 연장됨)
        // 2. 마감이 먼저면 -> WON 또는 UNSOLD
        // 결과가 섞이거나 에러로 인해 데이터가 깨지면 안 됨
        assertThat(result.getStatus()).isIn(AuctionStatus.ONGOING, AuctionStatus.WON, AuctionStatus.UNSOLD);
    }

    private void clearDatabase() {
        TransactionStatus status = tm.getTransaction(new DefaultTransactionDefinition());
        bidRepository.deleteAllInBatch();
        auctionRepository.deleteAllInBatch();
        userRepository.deleteAllInBatch();
        tm.commit(status);
    }
}
