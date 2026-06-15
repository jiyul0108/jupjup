package com.jupjup.Backend.global.exception;

import lombok.Getter;
import java.time.LocalDateTime;

@Getter
public class ErrorResponse {

    private final int status;
    private final String code;
    private final String message;
    private final LocalDateTime timestamp;

    private ErrorResponse(ErrorCode errorCode) {
        this.status    = errorCode.getHttpStatus().value();
        this.code      = errorCode.name();
        this.message   = errorCode.getMessage();
        this.timestamp = LocalDateTime.now();
    }

    private ErrorResponse(int status, String code, String message) {
        this.status    = status;
        this.code      = code;
        this.message   = message;
        this.timestamp = LocalDateTime.now();
    }

    public static ErrorResponse of(ErrorCode errorCode) {
        return new ErrorResponse(errorCode);
    }

    public static ErrorResponse of(int status, String code, String message) {
        return new ErrorResponse(status, code, message);
    }
}