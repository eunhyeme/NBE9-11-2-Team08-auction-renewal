package com.team8.backend.domain.auction.scheduler;

import com.team8.backend.domain.auction.entity.Auction;
import com.team8.backend.domain.auction.entity.AuctionStatus;
import com.team8.backend.domain.auction.repository.AuctionRepository;
import com.team8.backend.domain.order.repository.OrderRepository;
import com.team8.backend.domain.user.entity.User;
import com.team8.backend.domain.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import java.time.LocalDateTime;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class AuctionSchedulerIntegrationTest {

    @Autowired private AuctionScheduler auctionScheduler;
    @Autowired
    private AuctionRepository auctionRepository;
    @Autowired private OrderRepository orderRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private PlatformTransactionManager tm;

    @Test
    @DisplayName("경매가 100개 이상일 때 Slice(청크 처리) 루프를 통해 모두 정상 마감된다")
    void processExpiredAuctions_ChunkTest() {
        // Given: 120개의 만료된 경매 생성
        TransactionStatus status = tm.getTransaction(new DefaultTransactionDefinition());
        User seller = userRepository.save(User.create("s@t.com", "p", "S", "A"));
        for (int i = 0; i < 120; i++) {
            auctionRepository.save(Auction.builder()
                    .seller(seller).title("T"+i).startPrice(100L).currentPrice(100L).bidUnit(10)
                    .status(AuctionStatus.ONGOING).startedAt(LocalDateTime.now().minusDays(1))
                    .endedAt(LocalDateTime.now().minusMinutes(1)).build());
        }
        tm.commit(status);

        // When
        auctionScheduler.processExpiredAuctions();

        // Then
        long ongoingCount = auctionRepository.findAll().stream()
                .filter(a -> a.getStatus() == AuctionStatus.ONGOING).count();
        assertThat(ongoingCount).isEqualTo(0); // 120개 모두 마감되어야 함
    }
}
