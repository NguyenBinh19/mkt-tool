package com.example.extracttool.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.http.converter.HttpMessageNotReadableException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(AppException.class)
    ResponseEntity<ApiErrorResponse> handleAppException(AppException exception, HttpServletRequest request) {
        return response(exception.getErrorCode(), request);
    }
    @ExceptionHandler({MethodArgumentNotValidException.class, ConstraintViolationException.class})
    ResponseEntity<ApiErrorResponse> handleValidation(Exception exception, HttpServletRequest request) { return response(ErrorCode.VALIDATION_ERROR, request); }
    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ApiErrorResponse> handleMalformedRequest(HttpMessageNotReadableException exception, HttpServletRequest request) { return response(ErrorCode.MALFORMED_REQUEST, request); }
    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<ApiErrorResponse> handleAccessDenied(AccessDeniedException exception, HttpServletRequest request) { return response(ErrorCode.ACCESS_DENIED, request); }
    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiErrorResponse> handleUnexpected(Exception exception, HttpServletRequest request) {
        log.error("Unhandled request failure: {} {}", request.getMethod(), request.getRequestURI(), exception);
        return response(ErrorCode.INTERNAL_ERROR, request);
    }
    private ResponseEntity<ApiErrorResponse> response(ErrorCode errorCode, HttpServletRequest request) {
        return ResponseEntity.status(errorCode.getStatus()).body(ApiErrorResponse.from(errorCode, request.getRequestURI()));
    }
}
