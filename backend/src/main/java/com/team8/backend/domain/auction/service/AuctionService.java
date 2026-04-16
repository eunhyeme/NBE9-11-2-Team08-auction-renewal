package com.team8.backend.domain.auction.service;

import com.team8.backend.domain.auction.dto.request.AuctionCreateRequest;
import com.team8.backend.domain.auction.dto.request.AuctionUpdateRequest;
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
@Transactional(readOnly = true)
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
        LocalDateTime now = LocalDateTime.now();
        if (request.startedAt().isBefore(now)) {
            throw new CustomException(ErrorCode.AUCTION_START_PAST);
        }

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

    /**
     * 경매 철회 (Soft Delete)
     */
    @Transactional // 💡 데이터 상태를 변경하므로 ReadOnly 해제
    public void withdrawAuction(Long userId, Long auctionId) {
        // 1. 존재하는(삭제되지 않은) 경매인지 DB에서 바로 확인
        Auction auction = auctionRepository.findByAuctionIdAndDeletedAtIsNull(auctionId)
                .orElseThrow(() -> new CustomException(ErrorCode.AUCTION_NOT_FOUND));

        // 2. 작성자 본인 확인
        if (!auction.getSeller().getUserId().equals(userId)) {
            throw new CustomException(ErrorCode.ACCESS_DENIED);
        }

        // 3. 입찰 존재 여부 확인 (신뢰를 위해 입찰자가 생기면 철회 불가)
        if (auction.getLastBidder() != null) {
            throw new CustomException(ErrorCode.AUCTION_HAS_BID);
        }

        // 4. 엔티티 내부 로직을 통해 철회 처리 (deletedAt 기록)
        auction.withdraw();
    }

    /**
     * 경매 수정
     */
    @Transactional
    public void updateAuction(Long userId, Long auctionId, AuctionUpdateRequest request) {
        // 1. 존재하는 경매인지 확인
        Auction auction = auctionRepository.findByAuctionIdAndDeletedAtIsNull(auctionId)
                .orElseThrow(() -> new CustomException(ErrorCode.AUCTION_NOT_FOUND));

        // 2. 본인 확인 및 입찰 여부 체크
        if (!auction.getSeller().getUserId().equals(userId)) {
            throw new CustomException(ErrorCode.ACCESS_DENIED);
        }
        if (auction.getLastBidder() != null) {
            throw new CustomException(ErrorCode.AUCTION_HAS_BID);
        }

        // 3. 값이 있으면 수정본을, 없으면(null) 기존 값을 유지
        String title = (request.title() != null) ? request.title() : auction.getTitle();
        String description = (request.description() != null) ? request.description() : auction.getDescription();
        String imageUrl = (request.imageUrl() != null) ? request.imageUrl() : auction.getImageUrl();
        Long startPrice = (request.startPrice() != null) ? request.startPrice() : auction.getStartPrice();
        Integer bidUnit = (request.bidUnit() != null) ? request.bidUnit() : auction.getBidUnit();

        // 4. 경매 기간 리밸런싱
        // durationInDays가 왔을 때만 시작 시간 기준으로 종료 시간을 다시 계산
        LocalDateTime newEndedAt = (request.durationInDays() != null)
                ? auction.getStartedAt().plusDays(request.durationInDays())
                : auction.getEndedAt();

        if (newEndedAt.isBefore(LocalDateTime.now())) {
            throw new CustomException(ErrorCode.INVALID_AUCTION_TIME);
        }

        // 5. 전체 업데이트 실행 (JPA의 Dirty Checking으로 자동 반영)
        auction.update(
                title,
                description,
                imageUrl,
                startPrice,
                bidUnit,
                newEndedAt
        );
    }
}