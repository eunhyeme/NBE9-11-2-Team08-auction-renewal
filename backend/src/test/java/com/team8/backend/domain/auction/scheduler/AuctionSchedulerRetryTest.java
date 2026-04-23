package com.team8.backend.domain.auction.scheduler;

import com.team8.backend.domain.auction.entity.Auction;
import com.team8.backend.domain.auction.facade.AuctionFacade;
import com.team8.backend.domain.auction.repository.AuctionRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;

import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuctionSchedulerRetryTest {

    @InjectMocks
    private AuctionScheduler auctionScheduler;
    @Mock
    private AuctionRepository auctionRepository;
    @Mock private AuctionFacade auctionFacade;

    @Test
    @DisplayName("낙관적 락 충돌 시 스케줄러는 최대 3번까지 서비스 호출을 재시도한다")
    void retry_OnOptimisticLock() {
        // Given
        Auction mockAuction = mock(Auction.class);
        given(mockAuction.getAuctionId()).willReturn(1L);

        // 💡 Slice 객체 모킹
        Slice<Auction> mockSlice = new SliceImpl<>(List.of(mockAuction));

        // 💡 Pageable 파라미터가 포함된 메서드 모킹
        given(auctionRepository.findByStatusAndEndedAtBeforeAndDeletedAtIsNull(any(), any(), any()))
                .willReturn(mockSlice);

        // 두 번 실패 후 세 번째에 성공하도록 설정
        doThrow(new OptimisticLockingFailureException("충돌"))
                .doThrow(new OptimisticLockingFailureException("충돌"))
                .doNothing()
                .when(auctionFacade).closeAuction(1L);

        // When
        auctionScheduler.processExpiredAuctions();

        // Then: 락 충돌 시 재시도가 일어나 총 3번 호출되었는지 확인
        
        verify(auctionFacade, times(3)).closeAuction(1L);
    }
}
