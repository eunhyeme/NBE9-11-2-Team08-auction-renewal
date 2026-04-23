package com.team8.backend.domain.auction.scheduler;

import com.team8.backend.domain.auction.entity.Auction;
import com.team8.backend.domain.auction.entity.AuctionStatus;
import com.team8.backend.domain.auction.facade.AuctionFacade;
import com.team8.backend.domain.auction.repository.AuctionRepository;
import com.team8.backend.domain.auction.service.AuctionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuctionScheduler {

    private final AuctionRepository auctionRepository;
    private final AuctionService auctionService;
    private final AuctionFacade auctionFacade;

    // 매 초마다 실행
    @Scheduled(fixedDelay = 1000)
    // 매 분 0초에 실행 (서울 시간 기준)
//    @Scheduled(cron = "0 * * * * *", zone = "Asia/Seoul")
    public void processExpiredAuctions() {
        LocalDateTime now = LocalDateTime.now();
        int pageSize = 100; // 청크 단위
        int totalProcessed = 0; // 처리된 경매 수

        while (true) {
            // Slice를 사용하여 0페이지부터 데이터를 100개씩 가져옴
            Slice<Auction> expiredSlice = auctionRepository.findByStatusAndEndedAtBeforeAndDeletedAtIsNull(
                    AuctionStatus.ONGOING, now, PageRequest.of(0, pageSize)
            );

            if (expiredSlice.isEmpty()) break;

            // 각 경매에 대해 재시도 로직을 포함한 개별 마감 처리 실행
            expiredSlice.getContent().forEach(this::tryCloseAuction);

            // 처리된 건수 합산
            totalProcessed += expiredSlice.getNumberOfElements();

            // 다음 페이지가 없으면 루프 종료
            if (!expiredSlice.hasNext()) break;
        }

        // 처리된 건수가 있을 때만 로그 출력
        if (totalProcessed > 0) {
            log.info("경매 마감 스케줄러 작동: 총 {}건 마감 처리 완료", totalProcessed);
        }
    }

    private void tryCloseAuction(Auction auction) {
        int maxRetries = 3;
        for (int i = 0; i < maxRetries; i++) {
            try {
                auctionFacade.closeAuction(auction.getAuctionId());
                break; // 성공 시 루프 탈출
            } catch (OptimisticLockingFailureException e) {
                log.warn("낙관적 락 충돌 (재시도 {}/{}): {}", i + 1, maxRetries, auction.getAuctionId());
                if (i == maxRetries - 1) log.error("경매 {} 마감 최종 실패", auction.getAuctionId());
                else backoff(i);
            } catch (Exception e) {
                log.error("경매 {} 처리 중 예상치 못한 오류: {}", auction.getAuctionId(), e.getMessage());
                break;
            }
        }
    }

    private void backoff(int retryIndex) {
        try {
            // i가 0부터 시작하므로 (100ms, 200ms, 300ms...) 점진적 대기
            Thread.sleep(100L * (retryIndex + 1));
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }
}
