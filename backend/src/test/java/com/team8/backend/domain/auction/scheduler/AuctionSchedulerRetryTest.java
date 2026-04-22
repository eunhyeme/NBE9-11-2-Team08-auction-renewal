package com.team8.backend.domain.auction.scheduler;

import com.team8.backend.domain.auction.entity.Auction;
import com.team8.backend.domain.auction.repository.AuctionRepository;
import com.team8.backend.domain.auction.service.AuctionService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.OptimisticLockingFailureException;

import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuctionSchedulerRetryTest {

    @InjectMocks private AuctionScheduler auctionScheduler;
    @Mock private AuctionRepository auctionRepository;
    @Mock private AuctionService auctionService;

    @Test
    @DisplayName("낙관적 락 충돌 시 최대 3번까지 재시도한다")
    void retry_OnOptimisticLock() {
        // Given
        Auction mockAuction = mock(Auction.class);
        given(mockAuction.getAuctionId()).willReturn(1L);
        given(auctionRepository.findByStatusAndEndedAtBeforeAndDeletedAtIsNull(any(), any()))
                .willReturn(List.of(mockAuction));

        // 두 번 실패 후 세 번째에 성공하도록 설정
        doThrow(new OptimisticLockingFailureException("충돌"))
                .doThrow(new OptimisticLockingFailureException("충돌"))
                .doNothing()
                .when(auctionService).closeAuction(1L);

        // When
        auctionScheduler.processExpiredAuctions();

        // Then: closeAuction이 총 3번 호출되었는지 확인
        verify(auctionService, times(3)).closeAuction(1L);
    }
}
