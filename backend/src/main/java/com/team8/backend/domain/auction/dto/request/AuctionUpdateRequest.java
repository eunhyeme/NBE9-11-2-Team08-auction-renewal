package com.team8.backend.domain.auction.dto.request;

import com.team8.backend.domain.auction.util.AuctionValidator;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.PositiveOrZero;

public record AuctionUpdateRequest(
        String title,
        String description,
        String imageUrl,

        @PositiveOrZero(message = "시작가는 0원 이상이어야 합니다.") // 💡 시작가 양수 검증
        Long startPrice,

        @Min(value = 100, message = "입찰 단위는 최소 100원 이상이어야 합니다.") // 💡 입찰 단위 최소값
        Integer bidUnit,

        @Min(value = 1, message = "경매 기간은 최소 1일 이상이어야 합니다.") // 💡 기간 최소값
        Long durationInDays
) {
    @AssertTrue(message = "입찰 단위는 100원 단위로만 설정 가능합니다.")
    public boolean isBidUnitMultipleOf100() {
        return AuctionValidator.isBidUnitMultipleOf100(bidUnit);
    }
}