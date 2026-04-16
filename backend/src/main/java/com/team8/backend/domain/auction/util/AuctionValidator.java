package com.team8.backend.domain.auction.util;

public class AuctionValidator {
    // 100원 단위 검증 로직을 단일화
    public static boolean isBidUnitMultipleOf100(Integer bidUnit) {
        if (bidUnit == null) return true; // @NotNull에서 별도 처리하거나 수정 시 null 허용
        return bidUnit % 100 == 0;
    }
}