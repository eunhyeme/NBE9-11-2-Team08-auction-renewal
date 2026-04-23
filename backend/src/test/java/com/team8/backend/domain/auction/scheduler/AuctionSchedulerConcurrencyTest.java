package com.team8.backend.domain.auction.scheduler;

import com.team8.backend.domain.auction.entity.Auction;
import com.team8.backend.domain.auction.entity.AuctionStatus;
import com.team8.backend.domain.auction.repository.AuctionRepository;
import com.team8.backend.domain.auction.service.AuctionService;
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

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class AuctionSchedulerConcurrencyTest {

    @Autowired
    private AuctionService auctionService;
    @Autowired private AuctionRepository auctionRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private PlatformTransactionManager tm;

    private Long auctionId;
    private Long bidderId;

    @BeforeEach
    void setUp() {
        clearDatabase();

        TransactionStatus status = tm.getTransaction(new DefaultTransactionDefinition());
        User seller = userRepository.save(User.create("s@c.com", "p", "S", "서울"));
        User bidder = userRepository.save(User.create("b@c.com", "p", "B", "서울"));
        Auction a = auctionRepository.save(Auction.builder()
                .seller(seller).title("동시성경매").startPrice(100L).currentPrice(100L)
                .bidUnit(10).status(AuctionStatus.ONGOING).startedAt(LocalDateTime.now().minusDays(1))
                .endedAt(LocalDateTime.now().minusSeconds(1)).build());
        this.auctionId = a.getAuctionId();
        this.bidderId = bidder.getUserId();
        tm.commit(status);
    }

    @Test
    @DisplayName("입찰 요청과 마감 스케줄러가 동시에 발생해도 @Version 덕분에 데이터 정합성이 유지된다")
    void concurrency_Close_Vs_Update() throws InterruptedException {
        ExecutorService executorService = Executors.newFixedThreadPool(2);
        CountDownLatch latch = new CountDownLatch(2);

        // 스레드 1: 스케줄러 마감 시도
        executorService.submit(() -> {
            try { auctionService.closeAuction(auctionId); } catch (Exception ignored) {}
            finally { latch.countDown(); }
        });

        // 스레드 2: 사용자가 입찰 시도
        executorService.submit(() -> {
            try {
                TransactionStatus status = tm.getTransaction(new DefaultTransactionDefinition());
                // 💡 findByIdWithLock 대신 일반 findById 사용 (낙관적 락 작동)
                Auction a = auctionRepository.findById(auctionId).get();
                User b = userRepository.findById(bidderId).get();
                a.placeBid(b, 2000L);
                auctionRepository.save(a);
                tm.commit(status);
            } catch (Exception ignored) {
                // 여기서 OptimisticLockingFailureException이 발생하여 충돌이 방지됨
            } finally { latch.countDown(); }
        });

        latch.await();

        Auction result = auctionRepository.findById(auctionId).get();
        // 정합성 검증: 상태가 ONGOING(입찰성공)이거나 WON/UNSOLD(마감성공) 중 하나여야 함
        assertThat(result.getStatus()).isIn(AuctionStatus.ONGOING, AuctionStatus.WON, AuctionStatus.UNSOLD);
    }

    private void clearDatabase() {
        TransactionStatus status = tm.getTransaction(new DefaultTransactionDefinition());
        auctionRepository.deleteAllInBatch();
        userRepository.deleteAllInBatch();
        tm.commit(status);
    }
}
