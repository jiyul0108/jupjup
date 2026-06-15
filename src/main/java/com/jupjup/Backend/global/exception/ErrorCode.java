package com.jupjup.Backend.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // 공통
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "입력값이 올바르지 않습니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류가 발생했습니다."),

    // 인증 / 사용자
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."),
    EMAIL_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 사용 중인 이메일입니다."),
    NICKNAME_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 사용 중인 닉네임입니다."),
    INVALID_PASSWORD(HttpStatus.UNAUTHORIZED, "비밀번호가 올바르지 않습니다."),

    // 상품
    PRODUCT_NOT_FOUND(HttpStatus.NOT_FOUND, "상품을 찾을 수 없습니다."),
    PRODUCT_NOT_OWNER(HttpStatus.FORBIDDEN, "본인 상품만 수정/삭제할 수 있습니다."),

    // 채팅
    CHAT_ROOM_NOT_FOUND(HttpStatus.NOT_FOUND, "채팅방을 찾을 수 없습니다."),
    CHAT_ROOM_ACCESS_DENIED(HttpStatus.FORBIDDEN, "채팅방에 접근할 권한이 없습니다."),
    CHAT_SELF_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "본인 상품에는 채팅을 걸 수 없습니다."),

    // 리뷰
    REVIEW_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 리뷰를 작성하셨습니다."),
    REVIEW_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "거래완료된 상품만 리뷰를 작성할 수 있습니다."),
    REVIEW_SELLER_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "현재는 구매자만 리뷰를 작성할 수 있습니다."),

    // 신고
    REPORT_SELF_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "본인 상품은 신고할 수 없습니다."),
    REPORT_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 신고한 상품입니다.");

    private final HttpStatus httpStatus;
    private final String message;
}