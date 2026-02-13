package com.possystem.config;

import com.possystem.common.ApiResponse;
import com.possystem.common.ErrorCode;
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

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // ==================== 401 UNAUTHORIZED - Authentication Errors ====================
    // Use these when the user's identity cannot be verified (invalid/missing/expired token)

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiResponse<Void>> handleBadCredentials(BadCredentialsException ex) {
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error(ErrorCode.INVALID_CREDENTIALS, ex.getMessage(), HttpStatus.UNAUTHORIZED.value()));
    }

    @ExceptionHandler(ExpiredJwtException.class)
    public ResponseEntity<ApiResponse<Void>> handleExpiredJwt(ExpiredJwtException ex) {
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error(ErrorCode.EXPIRED_TOKEN, HttpStatus.UNAUTHORIZED.value()));
    }

    @ExceptionHandler(MalformedJwtException.class)
    public ResponseEntity<ApiResponse<Void>> handleMalformedJwt(MalformedJwtException ex) {
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error(ErrorCode.INVALID_TOKEN, HttpStatus.UNAUTHORIZED.value()));
    }

    @ExceptionHandler(SignatureException.class)
    public ResponseEntity<ApiResponse<Void>> handleSignatureException(SignatureException ex) {
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error(ErrorCode.INVALID_TOKEN, HttpStatus.UNAUTHORIZED.value()));
    }

    // ==================== 403 FORBIDDEN - Authorization Errors ====================
    // Use these when user is authenticated but lacks permission or account has restrictions

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDenied(AccessDeniedException ex) {
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error(ErrorCode.ACCESS_DENIED, HttpStatus.FORBIDDEN.value()));
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
        ErrorCode errorCode = determineIllegalArgumentErrorCode(ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(errorCode, ex.getMessage(), HttpStatus.BAD_REQUEST.value()));
    }

    // ==================== 409 CONFLICT - Duplicate/Conflict Errors ====================

    @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleDataIntegrity(
            org.springframework.dao.DataIntegrityViolationException ex) {
        ErrorCode errorCode = ErrorCode.DUPLICATE_ENTRY;
        String message = ex.getMessage();

        if (message != null) {
            if (message.contains("email")) {
                errorCode = ErrorCode.EMAIL_EXISTS;
            } else if (message.contains("phone")) {
                errorCode = ErrorCode.PHONE_EXISTS;
            } else if (message.contains("business_registration")) {
                errorCode = ErrorCode.BUSINESS_REG_EXISTS;
            }
        }

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(ApiResponse.error(errorCode, HttpStatus.CONFLICT.value()));
    }

    // ==================== 500 INTERNAL SERVER ERROR ====================

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGenericException(Exception ex) {
        ex.printStackTrace();
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
        if (message.contains("Shop is not active")) return ErrorCode.SHOP_INACTIVE;

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
        if (message.contains("Business registration")) return ErrorCode.BUSINESS_REG_EXISTS;

        return ErrorCode.VALIDATION_ERROR;
    }
}
