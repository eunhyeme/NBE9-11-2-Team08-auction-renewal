package com.team8.backend.domain.auction.facade;

import com.team8.backend.domain.auction.dto.ImageFile;
import com.team8.backend.domain.auction.dto.request.AuctionCreateRequest;
import com.team8.backend.domain.auction.dto.request.AuctionUpdateRequest;
import com.team8.backend.domain.auction.dto.response.AuctionResponse;
import com.team8.backend.domain.auction.entity.Auction;
import com.team8.backend.domain.auction.service.AuctionDomainService;
import com.team8.backend.domain.auction.service.ImageService;
import com.team8.backend.domain.user.entity.User;
import com.team8.backend.domain.user.repository.UserRepository;
import com.team8.backend.global.exception.CustomException;
import com.team8.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuctionFacadeImpl implements AuctionFacade {
    private final AuctionDomainService auctionDomainService;
    private final ImageService imageService;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public AuctionResponse createAuction(Long userId, AuctionCreateRequest dto, ImageFile image) {
        // 1. 이미지 저장 (ImageFile 사용)
        String imageUrl = imageService.upload(image, "auction");

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

        // 권한 및 입찰 여부 체크
        if (!auction.getSeller().getUserId().equals(userId)) {
            throw new CustomException(ErrorCode.ACCESS_DENIED);
        }
        if (auction.getLastBidder() != null) {
            throw new CustomException(ErrorCode.AUCTION_HAS_BID);
        }

        // 💡 이미지 처리 로직
        String imageUrl = auction.getImageUrl(); // 기본값은 기존 URL
        if (image != null) {
            imageService.delete(imageUrl); // 기존 파일 삭제
            imageUrl = imageService.upload(image, "auction"); // 새 파일 업로드
        }

        // 💡 새로운 이미지 URL을 DTO에 포함시켜 엔티티에 전달
        AuctionUpdateRequest updatedRequest;
        if (request != null) {
            // 수정 데이터가 들어온 경우: 해당 데이터에 imageUrl을 끼워넣음
            updatedRequest = request.withImageUrl(imageUrl);
        } else {
            // 수정 데이터가 없는 경우(이미지만 바꾼 경우): 기존 데이터 유지 + 새로운 imageUrl
            updatedRequest = new AuctionUpdateRequest(
                    auction.getTitle(),
                    auction.getDescription(),
                    imageUrl, // 💡 새 이미지 URL 반영
                    auction.getStartPrice(),
                    auction.getBidUnit(),
                    null // durationInDays는 업데이트하지 않음
            );
        }
        auction.update(updatedRequest); // 이제 파라미터가 하나인 메서드를 호출합니다.
    }
}