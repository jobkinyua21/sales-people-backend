package com.possystem.common;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // 401 - Authentication Errors (Invalid/Missing Token)
    INVALID_CREDENTIALS("AUTH_001", "Invalid email or password"),
    INVALID_TOKEN("AUTH_002", "Invalid or malformed token"),
    EXPIRED_TOKEN("AUTH_003", "Token has expired"),
    MISSING_TOKEN("AUTH_004", "Authentication token is required"),

    // 403 - Authorization Errors (Valid Token, Insufficient Permissions)
    ACCESS_DENIED("AUTHZ_001", "You do not have permission to access this resource"),
    INSUFFICIENT_PERMISSIONS("AUTHZ_002", "Insufficient permissions for this operation"),
    ACCOUNT_DISABLED("AUTHZ_003", "Account is disabled"),
    ACCOUNT_LOCKED("AUTHZ_004", "Account is locked"),
    ACCOUNT_PENDING("AUTHZ_005", "Account is pending activation"),
    ACCOUNT_SUSPENDED("AUTHZ_006", "Account is suspended"),
    ACCOUNT_DELETED("AUTHZ_007", "Account has been deleted"),
    SHOP_INACTIVE("AUTHZ_008", "Shop is not active"),

    // 400 - Validation Errors
    VALIDATION_ERROR("VAL_001", "Validation failed"),
    INVALID_REQUEST("VAL_002", "Invalid request format"),
    MISSING_REQUIRED_FIELD("VAL_003", "Required field is missing"),

    // 409 - Conflict Errors
    DUPLICATE_ENTRY("CONF_001", "A record with this information already exists"),
    EMAIL_EXISTS("CONF_002", "Email already registered"),
    PHONE_EXISTS("CONF_003", "Phone number already registered"),
    BUSINESS_REG_EXISTS("CONF_004", "Business registration number already exists"),

    // 404 - Not Found Errors
    RESOURCE_NOT_FOUND("NF_001", "Requested resource not found"),
    USER_NOT_FOUND("NF_002", "User not found"),
    SHOP_NOT_FOUND("NF_003", "Shop not found"),

    // 500 - Server Errors
    INTERNAL_ERROR("SRV_001", "An unexpected error occurred"),
    DATABASE_ERROR("SRV_002", "Database operation failed");

    private final String code;
    private final String defaultMessage;
}
