package com.salespeople.config;

import com.salespeople.common.ApiResponse;
import com.salespeople.common.ErrorCode;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.SignatureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // ==================== 403 FORBIDDEN - Token/Authentication Errors ====================

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiResponse<Void>> handleBadCredentials(BadCredentialsException ex) {
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error(ErrorCode.INVALID_CREDENTIALS, ex.getMessage(), HttpStatus.FORBIDDEN.value()));
    }

    @ExceptionHandler(ExpiredJwtException.class)
    public ResponseEntity<ApiResponse<Void>> handleExpiredJwt(ExpiredJwtException ex) {
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error(ErrorCode.EXPIRED_TOKEN, HttpStatus.FORBIDDEN.value()));
    }

    @ExceptionHandler(MalformedJwtException.class)
    public ResponseEntity<ApiResponse<Void>> handleMalformedJwt(MalformedJwtException ex) {
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error(ErrorCode.INVALID_TOKEN, HttpStatus.FORBIDDEN.value()));
    }

    @ExceptionHandler(SignatureException.class)
    public ResponseEntity<ApiResponse<Void>> handleSignatureException(SignatureException ex) {
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error(ErrorCode.INVALID_TOKEN, HttpStatus.FORBIDDEN.value()));
    }

    // ==================== 401 UNAUTHORIZED - Permission Errors ====================

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDenied(AccessDeniedException ex) {
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error(ErrorCode.ACCESS_DENIED, HttpStatus.UNAUTHORIZED.value()));
    }

    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<ApiResponse<Void>> handleDisabled(DisabledException ex) {
        ErrorCode errorCode = determineAccountErrorCode(ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error(errorCode, ex.getMessage(), HttpStatus.FORBIDDEN.value()));
    }

    @ExceptionHandler(LockedException.class)
    public ResponseEntity<ApiResponse<Void>> handleLocked(LockedException ex) {
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error(ErrorCode.ACCOUNT_LOCKED, ex.getMessage(), HttpStatus.FORBIDDEN.value()));
    }

    // ==================== 400 BAD REQUEST - Validation Errors ====================

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationErrors(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(ErrorCode.VALIDATION_ERROR, HttpStatus.BAD_REQUEST.value(), errors));
    }

    @ExceptionHandler(org.springframework.http.converter.HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleHttpMessageNotReadable(
            org.springframework.http.converter.HttpMessageNotReadableException ex) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(ErrorCode.INVALID_REQUEST, HttpStatus.BAD_REQUEST.value()));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalState(IllegalStateException ex) {
        ErrorCode errorCode = determineIllegalStateErrorCode(ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.TOO_MANY_REQUESTS)
                .body(ApiResponse.error(errorCode, ex.getMessage(), HttpStatus.TOO_MANY_REQUESTS.value()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgument(IllegalArgumentException ex) {
        log.error("IllegalArgumentException: {}", ex.getMessage(), ex);
        ErrorCode errorCode = determineIllegalArgumentErrorCode(ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(errorCode, ex.getMessage(), HttpStatus.BAD_REQUEST.value()));
    }

    // ==================== 409 CONFLICT - Duplicate/Conflict Errors ====================

    @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleDataIntegrity(
            org.springframework.dao.DataIntegrityViolationException ex) {
        log.error("Data integrity violation: {}", ex.getMostSpecificCause().getMessage());
        ErrorCode errorCode = ErrorCode.DUPLICATE_ENTRY;
        String message = ex.getMostSpecificCause().getMessage();

        if (message != null) {
            if (message.contains("email")) {
                errorCode = ErrorCode.EMAIL_EXISTS;
            } else if (message.contains("phone")) {
                errorCode = ErrorCode.PHONE_EXISTS;
            }
        }

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(ApiResponse.error(errorCode, message, HttpStatus.CONFLICT.value()));
    }

    // ==================== 500 INTERNAL SERVER ERROR ====================

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGenericException(Exception ex) {
        log.error("Unhandled exception", ex);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(ErrorCode.INTERNAL_ERROR, HttpStatus.INTERNAL_SERVER_ERROR.value()));
    }

    // ==================== Helper Methods ====================

    private ErrorCode determineAccountErrorCode(String message) {
        if (message == null) return ErrorCode.ACCOUNT_DISABLED;

        if (message.contains("pending")) return ErrorCode.ACCOUNT_PENDING;
        if (message.contains("suspended")) return ErrorCode.ACCOUNT_SUSPENDED;
        if (message.contains("deleted")) return ErrorCode.ACCOUNT_DELETED;
        if (message.contains("inactive")) return ErrorCode.ACCOUNT_DISABLED;

        return ErrorCode.ACCOUNT_DISABLED;
    }

    private ErrorCode determineIllegalStateErrorCode(String message) {
        if (message == null) return ErrorCode.VALIDATION_ERROR;

        if (message.contains("Too many OTP requests")) return ErrorCode.OTP_RATE_LIMIT;
        if (message.contains("Too many wrong attempts")) return ErrorCode.OTP_MAX_ATTEMPTS;

        return ErrorCode.VALIDATION_ERROR;
    }

    private ErrorCode determineIllegalArgumentErrorCode(String message) {
        if (message == null) return ErrorCode.VALIDATION_ERROR;

        if (message.contains("Email already")) return ErrorCode.EMAIL_EXISTS;
        if (message.contains("Phone number already")) return ErrorCode.PHONE_EXISTS;

        return ErrorCode.VALIDATION_ERROR;
    }
}
