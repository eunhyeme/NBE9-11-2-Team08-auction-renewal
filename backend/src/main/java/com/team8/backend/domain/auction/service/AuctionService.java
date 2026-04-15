package com.team8.backend.domain.auction.service;

import com.team8.backend.domain.auction.dto.request.AuctionCreateRequest;
import com.team8.backend.domain.auction.dto.response.AuctionResponse;
import com.team8.backend.domain.auction.entity.Auction;
import com.team8.backend.domain.auction.entity.AuctionStatus;
import com.team8.backend.domain.auction.repository.AuctionRepository;
import com.team8.backend.domain.user.entity.User;
import com.team8.backend.domain.user.repository.UserRepository;
import com.team8.backend.global.exception.CustomException;
import com.team8.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true) // 클래스 레벨은 기본적으로 ReadOnly
public class AuctionService {

    private final AuctionRepository auctionRepository;
    private final UserRepository userRepository;

    /**
     * 경매 목록 조회 (페이징)
     * 비인증 사용자도 접근 가능한 비즈니스 로직
     */
    public Page<AuctionResponse> getAuctions(Pageable pageable) {
        // 1. 삭제되지 않은 경매 목록 페이징 조회
        Page<Auction> auctions = auctionRepository.findAllByDeletedAtIsNull(pageable);

        // 2. Entity -> DTO 변환
        return auctions.map(AuctionResponse::from);
    }

    /**
     * 경매 단건 조회 로직
     */
    public AuctionResponse getAuction(Long auctionId) {
        // Repository에서 DB 필터링이 완료된 메서드를 호출
        Auction auction = auctionRepository.findByAuctionIdAndDeletedAtIsNull(auctionId)
                .orElseThrow(() -> new CustomException(ErrorCode.AUCTION_NOT_FOUND));

        return AuctionResponse.from(auction);
    }

    /**
     * 경매 상품 등록
     */
    @Transactional // 쓰기 작업이므로 ReadOnly 해제
    public AuctionResponse createAuction(Long userId, AuctionCreateRequest request) {
        // (기존 createAuction 로직 동일...)
        User seller = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        LocalDateTime start = request.startedAt().truncatedTo(ChronoUnit.SECONDS);
        LocalDateTime end = request.endedAt().truncatedTo(ChronoUnit.SECONDS);

        validateAuctionTime(start, end);

        Auction auction = Auction.builder()
                .seller(seller)
                .title(request.title())
                .imageUrl(request.imageUrl())
                .description(request.description())
                .startPrice(request.startPrice())
                .currentPrice(request.startPrice())
                .bidUnit(request.bidUnit())
                .status(AuctionStatus.ONGOING)
                .startedAt(start)
                .endedAt(end)
                .build();

        Auction savedAuction = auctionRepository.save(auction);
        return AuctionResponse.from(savedAuction);
    }

    private void validateAuctionTime(LocalDateTime start, LocalDateTime end) {
        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);

        if (start.isBefore(now)) {
            throw new CustomException(ErrorCode.INVALID_AUCTION_TIME);
        }

        long secondsBetween = ChronoUnit.SECONDS.between(start, end);
        long oneDayInSeconds = 24 * 60 * 60;

        if (secondsBetween < oneDayInSeconds ||
                secondsBetween > (oneDayInSeconds * 10) ||
                secondsBetween % oneDayInSeconds != 0) {
            throw new CustomException(ErrorCode.INVALID_AUCTION_TIME);
        }
    }
}