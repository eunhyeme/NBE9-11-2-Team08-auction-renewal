package com.team8.backend.domain.auction.service;

import com.team8.backend.domain.auction.dto.request.AuctionCreateRequest;
import com.team8.backend.domain.auction.dto.request.AuctionUpdateRequest;
import com.team8.backend.domain.auction.dto.response.AuctionResponse;
import com.team8.backend.domain.auction.entity.Auction;
import com.team8.backend.domain.auction.entity.AuctionStatus;
import com.team8.backend.domain.auction.repository.AuctionRepository;
import com.team8.backend.domain.auction.repository.AuctionSpecification;
import org.springframework.data.jpa.domain.Specification;
import com.team8.backend.domain.order.service.OrderService;
import com.team8.backend.domain.user.entity.User;
import com.team8.backend.domain.user.repository.UserRepository;
import com.team8.backend.global.exception.CustomException;
import com.team8.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuctionService {

    private final AuctionRepository auctionRepository;
    private final UserRepository userRepository;
    private final FileService fileService;
    private final OrderService orderService;

    /**
     * 경매 목록 조회 (페이징)
     * 비인증 사용자도 접근 가능한 비즈니스 로직
     */
    public Page<AuctionResponse> getAuctions(
            Pageable pageable, String keyword, Long minPrice, Long maxPrice, List<AuctionStatus> statuses) {
        Specification<Auction> spec = AuctionSpecification.notDeleted();

        List<AuctionStatus> allowedStatuses = (statuses == null || statuses.isEmpty())
                ? List.of(AuctionStatus.ONGOING)
                : statuses.stream().filter(s -> s != AuctionStatus.CANCELED).toList();
        if (allowedStatuses.isEmpty()) {
            allowedStatuses = List.of(AuctionStatus.ONGOING);
        }
        spec = spec.and(AuctionSpecification.statusIn(allowedStatuses));

        if (keyword != null && !keyword.isBlank()) {
            spec = spec.and(AuctionSpecification.titleContains(keyword));
        }
        if (minPrice != null) {
            spec = spec.and(AuctionSpecification.minPrice(minPrice));
        }
        if (maxPrice != null) {
            spec = spec.and(AuctionSpecification.maxPrice(maxPrice));
        }
        return auctionRepository.findAll(spec, pageable).map(AuctionResponse::from);
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

    public List<AuctionResponse> getMyAuctions(Long userId) {
        // 서비스에서 조회와 변환을 맡아 컨트롤러를 단순하게 유지합니다:
        // 컨트롤러는 인증된 판매자 ID만 넘기고, 서비스가 엔티티를 응답 DTO로 변환합니다.
        return auctionRepository.findAllBySeller_UserIdAndDeletedAtIsNullOrderByStartedAtDesc(userId).stream()
                .map(AuctionResponse::from)
                .toList();
    }

    public AuctionResponse getMyAuction(Long userId, Long auctionId) {
        Auction auction = auctionRepository.findByAuctionIdAndSeller_UserIdAndDeletedAtIsNull(auctionId, userId)
                .orElseThrow(() -> new CustomException(ErrorCode.AUCTION_NOT_FOUND));

        return AuctionResponse.from(auction);
    }

    /**
     * 경매 상품 등록
     */
    @Transactional
    public AuctionResponse createAuction(Long userId, AuctionCreateRequest request, org.springframework.web.multipart.MultipartFile image) throws java.io.IOException {
        // 1. 등록 시 이미지는 필수
        if (image == null || image.isEmpty()) {
            throw new CustomException(ErrorCode.IMAGE_REQUIRED);
        }

        // 2. 파일 저장 및 URL 반환
        String savedImageUrl = fileService.saveFile(image);

        User seller = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        LocalDateTime start = request.startedAt().truncatedTo(ChronoUnit.SECONDS);
        LocalDateTime end = request.endedAt().truncatedTo(ChronoUnit.SECONDS);

        validateAuctionTime(start, end);

        Auction auction = Auction.builder()
                .seller(seller)
                .title(request.title())
                .imageUrl(savedImageUrl)
                .description(request.description())
                .startPrice(request.startPrice())
                .currentPrice(request.startPrice())
                .bidUnit(request.bidUnit())
                .status(AuctionStatus.ONGOING)
                .startedAt(start)
                .endedAt(end)
                .build();

        return AuctionResponse.from(auctionRepository.save(auction));
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
    @Transactional
    public void withdrawAuction(Long userId, Long auctionId) {
        // 1. 존재하는 경매인지 확인
        Auction auction = auctionRepository.findByAuctionIdAndDeletedAtIsNull(auctionId)
                .orElseThrow(() -> new CustomException(ErrorCode.AUCTION_NOT_FOUND));

        // 2. 작성자 본인 확인
        if (!auction.getSeller().getUserId().equals(userId)) {
            throw new CustomException(ErrorCode.ACCESS_DENIED);
        }

        // 3. 입찰 존재 여부 확인
        if (auction.getLastBidder() != null) {
            throw new CustomException(ErrorCode.AUCTION_HAS_BID);
        }

        // 4. 물리 파일 삭제 로직 추가
        // 데이터는 Soft Delete로 남지만, 용량 관리를 위해 이미지는 삭제합니다.
        fileService.deleteFile(auction.getImageUrl());

        // 5. 엔티티 내부 로직을 통해 철회 처리
        auction.withdraw();
    }

    /**
     * 경매 수정
     */
    @Transactional
    public void updateAuction(Long userId, Long auctionId, AuctionUpdateRequest request, org.springframework.web.multipart.MultipartFile image) throws java.io.IOException {
        Auction auction = auctionRepository.findByAuctionIdAndDeletedAtIsNull(auctionId)
                .orElseThrow(() -> new CustomException(ErrorCode.AUCTION_NOT_FOUND));

        if (!auction.getSeller().getUserId().equals(userId)) {
            throw new CustomException(ErrorCode.ACCESS_DENIED);
        }
        if (auction.getLastBidder() != null) {
            throw new CustomException(ErrorCode.AUCTION_HAS_BID);
        }

        // 1. 이미지 교체 로직
        String imageUrl = auction.getImageUrl();
        if (image != null && !image.isEmpty()) {
            fileService.deleteFile(auction.getImageUrl());
            imageUrl = fileService.saveFile(image);
        }

        // 2. 리뷰 반영: request 및 필드 Null 체크 강화
        // request 자체가 null일 경우 NPE가 발생하는 것을 방지합니다.
        String title = (request != null && request.title() != null) ? request.title() : auction.getTitle();
        String description = (request != null && request.description() != null) ? request.description() : auction.getDescription();
        Long startPrice = (request != null && request.startPrice() != null) ? request.startPrice() : auction.getStartPrice();
        Integer bidUnit = (request != null && request.bidUnit() != null) ? request.bidUnit() : auction.getBidUnit();

        LocalDateTime newEndedAt = auction.getEndedAt();
        if (request != null && request.durationInDays() != null) {
            newEndedAt = auction.getStartedAt().plusDays(request.durationInDays());
        }

        if (newEndedAt.isBefore(LocalDateTime.now())) {
            throw new CustomException(ErrorCode.INVALID_AUCTION_TIME);
        }

        auction.update(title, description, imageUrl, startPrice, bidUnit, newEndedAt);
    }

    // [스케줄러] 경매 마감 처리
    @Transactional(propagation = Propagation.REQUIRES_NEW) // 각 경매 마감 처리를 독립적인 트랜잭션으로 실행
    public void closeAuction(Long auctionId) {
        // 존재하는 경매인지 확인 (락 사용)
        Auction auction = auctionRepository.findByIdWithLock(auctionId)
                .orElseThrow(() -> new CustomException(ErrorCode.AUCTION_NOT_FOUND));

        // 이미 마감된 경매라면 종료해서 중복 주문 생성 방지
        if (auction.getStatus() != AuctionStatus.ONGOING) {
            return;
        }

        // 낙찰자 존재 여부에 따라 낙찰(WON) or 유찰(UNSOLD)
        auction.close();

        // 낙찰된 경우 오더 생성
        if (auction.getStatus() == AuctionStatus.WON) {
            orderService.createOrder(auction, auction.getLastBidder(), auction.getCurrentPrice());
        }
    }
}
