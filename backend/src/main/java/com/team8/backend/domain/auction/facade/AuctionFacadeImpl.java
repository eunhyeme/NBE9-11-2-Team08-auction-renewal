package com.team8.backend.domain.auction.facade;

import com.team8.backend.domain.auction.dto.ImageFile;
import com.team8.backend.domain.auction.dto.request.AuctionCreateRequest;
import com.team8.backend.domain.auction.dto.request.AuctionUpdateRequest;
import com.team8.backend.domain.auction.dto.response.AuctionResponse;
import com.team8.backend.domain.auction.entity.Auction;
import com.team8.backend.domain.auction.entity.AuctionStatus;
import com.team8.backend.domain.auction.event.AuctionEvent;
import com.team8.backend.domain.auction.event.ImageUploadEvent;
import com.team8.backend.domain.auction.repository.AuctionRepository;
import com.team8.backend.domain.auction.service.AuctionDomainService;
import com.team8.backend.domain.auction.service.ImageService;
import com.team8.backend.domain.order.service.OrderService;
import com.team8.backend.domain.user.entity.User;
import com.team8.backend.domain.user.repository.UserRepository;
import com.team8.backend.global.exception.CustomException;
import com.team8.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuctionFacadeImpl implements AuctionFacade {
    private final AuctionDomainService auctionDomainService;
    private final ImageService imageService;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final AuctionRepository auctionRepository;
    private final OrderService orderService;

    @Override
    @Transactional
    public AuctionResponse createAuction(Long userId, AuctionCreateRequest dto, ImageFile image) {
        // 1. 이미지 저장 (ImageFile 사용)
        String imageUrl = imageService.upload(image, "auction");

        // 롤백 대비 이벤트 발생
        eventPublisher.publishEvent(new ImageUploadEvent(imageUrl));

        // 2. 판매자 정보 조회
        User seller = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // 3. DTO -> Entity 변환 및 저장
        Auction auction = dto.toEntity(seller, imageUrl);
        Auction savedAuction = auctionDomainService.save(auction);

        return AuctionResponse.from(savedAuction);
    }

    @Override
    @Transactional
    public void deleteAuction(Long userId, Long auctionId) {
        // 1. 경매 존재 확인 및 조회
        Auction auction = auctionDomainService.getById(auctionId);

        // 2. 권한 확인 (본인의 경매인지 확인)
        // 💡 ErrorCode.ACCESS_DENIED 사용
        if (!auction.getSeller().getUserId().equals(userId)) {
            throw new CustomException(ErrorCode.ACCESS_DENIED);
        }

        // 3. 입찰자가 있는 경우 삭제 불가
        // 💡 ErrorCode.AUCTION_HAS_BID 사용
        if (auction.getLastBidder() != null) {
            throw new CustomException(ErrorCode.AUCTION_HAS_BID);
        }

        // 4. 실제 이미지 파일 삭제 (서버 용량 관리)
        imageService.delete(auction.getImageUrl());

        // 5. DB 데이터 삭제
        auctionDomainService.delete(auction);
    }

    @Override
    @Transactional
    public void updateAuction(Long userId, Long auctionId, AuctionUpdateRequest request, ImageFile image) {
        Auction auction = auctionDomainService.getById(auctionId);

        // 1. 권한 및 입찰 여부 체크 (기존 로직 유지)
        if (!auction.getSeller().getUserId().equals(userId)) {
            throw new CustomException(ErrorCode.ACCESS_DENIED);
        }
        if (auction.getLastBidder() != null) {
            throw new CustomException(ErrorCode.AUCTION_HAS_BID);
        }

        String oldImageUrl = auction.getImageUrl();
        String imageUrl = oldImageUrl; // 기본값은 기존 URL

        if (image != null) {
            // 💡 2. 새 이미지 먼저 업로드 (성공해야 다음 단계로 진행)
            String newImageUrl = imageService.upload(image, "auction");

            // 롤백 대비 이벤트 발생
            eventPublisher.publishEvent(new ImageUploadEvent(newImageUrl));

            imageUrl = newImageUrl;
        }

        // 4. 새로운 이미지 URL을 포함하여 DTO 재구성 및 엔티티 업데이트
        AuctionUpdateRequest updatedRequest;
        if (request != null) {
            // 💡 데이터가 들어온 경우에도, 개별 필드가 null이면 기존 값을 가져다 씁니다.
            updatedRequest = new AuctionUpdateRequest(
                    request.title() != null ? request.title() : auction.getTitle(),
                    request.description() != null ? request.description() : auction.getDescription(),
                    imageUrl,
                    request.startPrice() != null ? request.startPrice() : auction.getStartPrice(),
                    request.bidUnit() != null ? request.bidUnit() : auction.getBidUnit(),
                    request.durationInDays()
            );
        } else {
            // 이미지만 바꾼 경우 (기존 로직 유지)
            updatedRequest = new AuctionUpdateRequest(
                    auction.getTitle(), auction.getDescription(), imageUrl,
                    auction.getStartPrice(), auction.getBidUnit(), null
            );
        }

        auction.update(updatedRequest);
        imageService.delete(oldImageUrl);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void closeAuction(Long auctionId) {
        Auction auction = auctionRepository.findByAuctionIdAndDeletedAtIsNull(auctionId)
                .orElseThrow(() -> new CustomException(ErrorCode.AUCTION_NOT_FOUND));

        if (auction.getStatus() != AuctionStatus.ONGOING) {
            return;
        }

        auction.close();

        if (auction.getStatus() == AuctionStatus.WON) {
            orderService.createOrder(auction, auction.getLastBidder(), auction.getCurrentPrice());
        }

        // 성공시 웹소켓 연결 끊는 이벤트 발행 (트랜잭션 성공 시에만)
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                eventPublisher.publishEvent(new AuctionEvent(auction.getAuctionId()));
            }
        });
    }
}