package com.team8.backend.domain.auction.scheduler;

import com.team8.backend.domain.auction.entity.Auction;
import com.team8.backend.domain.auction.entity.AuctionStatus;
import com.team8.backend.domain.auction.repository.AuctionRepository;
import com.team8.backend.domain.auction.service.AuctionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;


@Slf4j
@Component
@RequiredArgsConstructor
public class AuctionScheduler {

    private final AuctionRepository auctionRepository;
    private final AuctionService auctionService;

    // 매 분 0초에 실행 (서울 시간 기준)
    @Scheduled(cron = "0 * * * * *", zone = "Asia/Seoul")
    public void processExpiredAuctions() {
        LocalDateTime now = LocalDateTime.now();
        List<Auction> expiredAuctions = auctionRepository.findByStatusAndEndedAtBeforeAndDeletedAtIsNull(
                AuctionStatus.ONGOING, now
        );
        //  벌크로 가져온 뒤 for 단건 처리 x

        expiredAuctions.forEach(auction -> {
            int maxRetries = 3;
            for (int i = 0; i < maxRetries; i++) {
                try {
                    // 개별 트랜잭션 실행
                    auctionService.closeAuction(auction.getAuctionId());
                    break; // 성공 시 for 루프 탈출 (다음 경매로)

                } catch (OptimisticLockingFailureException e) {
                    log.warn("낙관적 락 충돌 발생 (재시도 {}/{}): 경매 ID {}", i + 1, maxRetries, auction.getAuctionId(), e); // 예외 객체 로깅

                    if (i == maxRetries - 1) {
                        log.error("경매 {} 마감 최종 실패 (최대 재시도 초과)", auction.getAuctionId(), e); // 예외 객체 로깅
                        // 💡 여기서 throw 하지 않음 -> 다음 경매 처리를 위해!
                    } else {
                        backoff(i);
                    }

                } catch (Exception e) {
                    log.error("경매 {} 처리 중 예상치 못한 오류: {}", auction.getAuctionId(), e.getMessage(), e); // 예외 객체 로깅
                    break; // 락 충돌 외의 에러는 재시도 의미가 없으므로 루프 탈출
                }
            }
        });
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
