package com.example.extracttool.exception;

import java.time.Instant;

public record ApiErrorResponse(String code, String message, Instant timestamp, String path) {
    public static ApiErrorResponse from(ErrorCode errorCode, String path) { return new ApiErrorResponse(errorCode.getCode(), errorCode.getMessage(), Instant.now(), path); }
}
