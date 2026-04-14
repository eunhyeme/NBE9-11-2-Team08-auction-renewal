package com.team8.backend.domain.auction.service;

import com.team8.backend.domain.auction.dto.request.AuctionCreateRequest;
import com.team8.backend.domain.auction.dto.response.AuctionResponse;
import com.team8.backend.domain.auction.entity.Auction;
import com.team8.backend.domain.auction.entity.AuctionStatus;
import com.team8.backend.domain.auction.repository.AuctionRepository;
import com.team8.backend.domain.user.entity.User;
import com.team8.backend.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuctionService {

    private final AuctionRepository auctionRepository;
    private final UserRepository userRepository;

    /**
     * 경매 상품 등록
     */
    @Transactional
    public AuctionResponse createAuction(Long userId, AuctionCreateRequest request) {
        // 1. 판매자 존재 여부 확인
        User seller = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));

        // 2. 시간 데이터 정규화 (나노초 절삭하여 비교 오류 방지)
        LocalDateTime start = request.startedAt().truncatedTo(ChronoUnit.SECONDS);
        LocalDateTime end = request.endedAt().truncatedTo(ChronoUnit.SECONDS);

        // 3. 비즈니스 로직 검증 (1일 단위, 최대 10일)
        validateAuctionTime(start, end);

        // 4. 경매 엔티티 생성 (ERD 기준: 생성 시 즉시 ONGOING)
        Auction auction = Auction.builder()
                .seller(seller)
                .title(request.title())
                .imageUrl(request.imageUrl())
                .description(request.description())
                .startPrice(request.startPrice())
                .currentPrice(request.startPrice()) // 초기 최고가는 시작가와 동일
                .bidUnit(request.bidUnit())
                .status(AuctionStatus.ONGOING) // ERD 표준 상태값 사용
                .startedAt(start)
                .endedAt(end)
                .build();

        // 5. 저장 및 응답 DTO 변환
        Auction savedAuction = auctionRepository.save(auction);
        return AuctionResponse.from(savedAuction);
    }

    private void validateAuctionTime(LocalDateTime start, LocalDateTime end) {
        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);

        // 시작 시간 검증
        if (start.isBefore(now)) {
            throw new IllegalArgumentException("경매 시작 시간은 현재 시간보다 과거일 수 없습니다.");
        }

        // 기간 계산 (초 단위)
        long secondsBetween = ChronoUnit.SECONDS.between(start, end);
        long oneDayInSeconds = 24 * 60 * 60; // 86400초

        // 1일 단위 및 범위(1~10일) 검증
        if (secondsBetween < oneDayInSeconds ||
                secondsBetween > (oneDayInSeconds * 10) ||
                secondsBetween % oneDayInSeconds != 0) {
            throw new IllegalArgumentException("경매 기간은 최소 1일에서 최대 10일 사이의 1일 단위여야 합니다.");
        }
    }
}