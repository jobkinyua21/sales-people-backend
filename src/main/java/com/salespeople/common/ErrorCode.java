package com.salespeople.common;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // 403 - Authentication Errors
    INVALID_CREDENTIALS("AUTH_001", "Invalid email or password"),
    INVALID_TOKEN("AUTH_002", "Invalid or malformed token"),
    EXPIRED_TOKEN("AUTH_003", "Token has expired"),
    MISSING_TOKEN("AUTH_004", "Authentication token is required"),
    PASSWORD_EXPIRED("AUTH_005", "Password has expired. Please reset your password"),
    PASSWORD_REUSED("AUTH_006", "Cannot reuse a recent password"),
    INVALID_RESET_TOKEN("AUTH_007", "Invalid or expired password reset token"),
    EMAIL_NOT_VERIFIED("AUTH_008", "Email address has not been verified"),
    EMAIL_ALREADY_VERIFIED("AUTH_009", "Email address is already verified"),
    INVALID_VERIFICATION_TOKEN("AUTH_010", "Invalid or expired verification token"),
    SESSION_EXPIRED("AUTH_011", "Session has expired"),
    MUST_CHANGE_PASSWORD("AUTH_012", "Password change is required"),
    INVALID_REFRESH_TOKEN("AUTH_013", "Invalid or expired refresh token"),
    INVALID_OTP("AUTH_014", "Invalid or expired OTP code"),
    OTP_REQUIRED("AUTH_015", "OTP verification required"),
    OTP_RATE_LIMIT("AUTH_016", "Too many OTP requests. Please try again later"),
    OTP_MAX_ATTEMPTS("AUTH_017", "Too many wrong attempts. OTP has been invalidated"),

    // 401 - Authorization Errors
    ACCESS_DENIED("AUTHZ_001", "You do not have permission to access this resource"),
    INSUFFICIENT_PERMISSIONS("AUTHZ_002", "Insufficient permissions for this operation"),
    ACCOUNT_DISABLED("AUTHZ_003", "Account is disabled"),
    ACCOUNT_LOCKED("AUTHZ_004", "Account is locked"),
    ACCOUNT_PENDING("AUTHZ_005", "Account is pending activation"),
    ACCOUNT_SUSPENDED("AUTHZ_006", "Account is suspended"),
    ACCOUNT_DELETED("AUTHZ_007", "Account has been deleted"),

    // 400 - Validation Errors
    VALIDATION_ERROR("VAL_001", "Validation failed"),
    INVALID_REQUEST("VAL_002", "Invalid request format"),
    MISSING_REQUIRED_FIELD("VAL_003", "Required field is missing"),

    // 409 - Conflict Errors
    DUPLICATE_ENTRY("CONF_001", "A record with this information already exists"),
    EMAIL_EXISTS("CONF_002", "Email already registered"),
    PHONE_EXISTS("CONF_003", "Phone number already registered"),

    // 409 - Role Conflicts
    ROLE_IN_USE("CONF_005", "Role is assigned to users and cannot be deleted"),
    SYSTEM_ROLE_IMMUTABLE("CONF_006", "System roles cannot be modified or deleted"),
    DUPLICATE_ROLE_NAME("CONF_007", "A role with this name already exists"),

    // 404 - Not Found Errors
    RESOURCE_NOT_FOUND("NF_001", "Requested resource not found"),
    USER_NOT_FOUND("NF_002", "User not found"),
    ROLE_NOT_FOUND("NF_004", "Role not found"),
    PERMISSION_NOT_FOUND("NF_005", "Permission not found"),

    // 500 - Server Errors
    INTERNAL_ERROR("SRV_001", "An unexpected error occurred"),
    DATABASE_ERROR("SRV_002", "Database operation failed");

    private final String code;
    private final String defaultMessage;
}
