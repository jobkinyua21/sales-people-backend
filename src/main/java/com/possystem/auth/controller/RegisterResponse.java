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
@Schema(description = "Registration response containing user and tenant information")
public class RegisterResponse {

    @Schema(description = "User's unique identifier")
    private UUID userId;

    @Schema(description = "Tenant's unique identifier (for self-onboarding)")
    private UUID tenantId;

    @Schema(description = "User's email address", example = "john.doe@business.com")
    private String email;

    @Schema(description = "User's full name", example = "John Doe")
    private String fullName;

    @Schema(description = "User type", example = "TENANT")
    private String userType;

    @Schema(description = "Account status", example = "PENDING")
    private String status;

    @Schema(description = "Business name (for self-onboarding)", example = "Acme Retail Store")
    private String businessName;

    @Schema(description = "Registration message", example = "Registration successful. Please check your email for verification.")
    private String message;
}
