package com.team8.backend.domain.auction.dto.request;

import com.team8.backend.domain.auction.entity.Auction;
import com.team8.backend.domain.auction.entity.AuctionStatus;
import com.team8.backend.domain.auction.util.AuctionValidator;
import com.team8.backend.domain.user.entity.User;
import com.team8.backend.global.exception.CustomException;
import com.team8.backend.global.exception.ErrorCode;
import jakarta.validation.constraints.*;

import java.time.LocalDateTime;

public record AuctionCreateRequest(
        @NotBlank(message = "경매 제목은 필수입니다.")
        String title,

        @NotBlank(message = "상품 설명은 필수입니다.")
        String description,

        @NotNull(message = "시작가는 필수입니다.")
        @PositiveOrZero(message = "시작가는 0원 이상이어야 합니다.")
        Long startPrice,

        @NotNull(message = "입찰 단위는 필수입니다.")
        @Min(value = 100, message = "입찰 단위는 최소 100원 이상이어야 합니다.")
        Integer bidUnit,

        @NotNull(message = "경매 시작 시간은 필수입니다.")
        LocalDateTime startedAt,

        @NotNull(message = "경매 종료 시간은 필수입니다.")
        LocalDateTime endedAt
) {
        @AssertTrue(message = "입찰 단위는 100원 단위로만 설정 가능합니다.")
        public boolean isBidUnitMultipleOf100() {
                return AuctionValidator.isBidUnitMultipleOf100(bidUnit);
        }

        @AssertTrue(message = "경매 종료 시간은 시작 시간 이후여야 합니다.")
        public boolean isEndTimeAfterStartTime() {
                return endedAt.isAfter(startedAt);
        }

        public Auction toEntity(User seller, String imageUrl) {
                if (imageUrl == null || imageUrl.isBlank()) {
                        throw new CustomException(ErrorCode.IMAGE_REQUIRED);
                }
                return Auction.builder()
                        .seller(seller)
                        .title(this.title)
                        .description(this.description)
                        .imageUrl(imageUrl)
                        .startPrice(this.startPrice)
                        .currentPrice(this.startPrice)
                        .bidUnit(this.bidUnit)
                        .status(AuctionStatus.ONGOING)
                        .startedAt(this.startedAt)
                        .endedAt(this.endedAt)
                        .build();
        }
}