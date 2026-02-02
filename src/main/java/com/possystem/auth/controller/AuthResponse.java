package com.possystem.auth.controller;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Authentication response containing JWT tokens and user information")
public class AuthResponse {

    @Schema(description = "JWT access token for API authentication", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
    private String accessToken;

    @Schema(description = "JWT refresh token for obtaining new access tokens", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
    private String refreshToken;

    @Schema(description = "Token type", example = "Bearer")
    private String tokenType;

    @Schema(description = "Access token expiration time in seconds", example = "86400")
    private Long expiresIn;

    @Schema(description = "User type: ADMIN, TENANT, or POS", example = "TENANT")
    private String userType;

    @Schema(description = "User's unique identifier")
    private UUID userId;

    @Schema(description = "User's email address", example = "user@example.com")
    private String email;

    @Schema(description = "User's full name", example = "John Doe")
    private String fullName;

    @Schema(description = "Tenant ID (only for POS users)")
    private UUID tenantId;

    @Schema(description = "Shop ID (only for POS users)")
    private UUID shopId;

    @Schema(description = "Shop name (only for POS users)", example = "Main Street Store")
    private String shopName;
}
