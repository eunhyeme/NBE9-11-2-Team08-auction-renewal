package com.team8.backend.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // COMMON
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "400-1", "잘못된 요청입니다."),
    VALIDATION_ERROR(HttpStatus.BAD_REQUEST, "400-2", "입력값을 확인해주세요."),
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "404-1", "요청한 데이터를 찾을 수 없습니다."),
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "403-1", "접근 권한이 없습니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "401-1", "인증이 필요합니다."),
    CONFLICT(HttpStatus.CONFLICT, "409-1", "요청이 충돌되었습니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "500-1", "서버 오류가 발생했습니다."),

    // MEMBERSHIP
    ALREADY_APPLIED(HttpStatus.BAD_REQUEST, "400-3", "이미 신청이 완료되었습니다."),
    INVALID_APPLICATION_STATE(HttpStatus.BAD_REQUEST, "400-4", "이미 처리된 신청입니다."),

    // USER
    DUPLICATE_EMAIL(HttpStatus.BAD_REQUEST, "400-5", "이미 사용 중인 이메일입니다."),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "404-2", "사용자를 찾을 수 없습니다."),
    INVALID_CREDENTIAL(HttpStatus.UNAUTHORIZED, "401-2", "이메일 또는 비밀번호를 확인해주세요."),

    // ORDER
    INVALID_ORDER_STATE(HttpStatus.BAD_REQUEST, "400-6", "주문 상태가 올바르지 않습니다."),
    PAYMENT_EXPIRED(HttpStatus.BAD_REQUEST, "400-7", "결제 시간이 만료되었습니다."),
    ALREADY_PAID(HttpStatus.BAD_REQUEST, "400-8", "이미 결제 완료된 주문입니다."),
    TRACKING_ALREADY_EXISTS(HttpStatus.BAD_REQUEST, "400-9", "이미 송장이 등록되어 있습니다."),
    ORDER_NOT_FOUND(HttpStatus.NOT_FOUND, "404-3", "주문을 찾을 수 없습니다."),
    SHIPPING_ADDRESS_MISSING(HttpStatus.BAD_REQUEST, "400-17", "배송지 정보가 입력되지 않았습니다."),
    DELIVERY_EXPIRED(HttpStatus.BAD_REQUEST, "400-18", "발송 기한이 만료되었습니다."),
    SHIPPING_ALREADY_STARTED(HttpStatus.BAD_REQUEST, "400-19", "이미 배송이 시작되어 주소를 변경할 수 없습니다."),
    // BID
    BID_TOO_LOW(HttpStatus.BAD_REQUEST, "400-10", "입찰 금액이 너무 낮습니다."),
    INVALID_BID_UNIT(HttpStatus.BAD_REQUEST, "400-11", "최소 입찰 단위를 지켜주세요."),
    SELF_BIDDING(HttpStatus.BAD_REQUEST, "400-12", "본인 경매에는 입찰할 수 없습니다."),
    CONCURRENT_BID(HttpStatus.CONFLICT, "409-2", "입찰이 동시에 처리되었습니다. 다시 시도해주세요."),
    // AUCTION
    INVALID_AUCTION_STATE(HttpStatus.BAD_REQUEST, "400-13", "종료된 경매입니다."),
    AUCTION_ALREADY_STARTED(HttpStatus.BAD_REQUEST, "400-14", "이미 시작된 경매는 수정할 수 없습니다."),
    AUCTION_HAS_BID(HttpStatus.BAD_REQUEST, "400-15", "입찰이 존재하여 수정/삭제할 수 없습니다."),
    AUCTION_NOT_FOUND(HttpStatus.NOT_FOUND, "404-4", "경매를 찾을 수 없습니다."),
    INVALID_AUCTION_TIME(HttpStatus.BAD_REQUEST, "400-16", "경매 시간이 유효하지 않습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
