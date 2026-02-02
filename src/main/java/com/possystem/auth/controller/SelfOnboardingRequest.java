package com.possystem.auth.controller;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Self-onboarding request for new tenants with business details")
public class SelfOnboardingRequest {

    // Personal Details
    @NotBlank(message = "First name is required")
    @Size(min = 2, max = 50, message = "First name must be between 2 and 50 characters")
    @Schema(description = "Tenant's first name", example = "John", requiredMode = Schema.RequiredMode.REQUIRED)
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Size(min = 2, max = 50, message = "Last name must be between 2 and 50 characters")
    @Schema(description = "Tenant's last name", example = "Doe", requiredMode = Schema.RequiredMode.REQUIRED)
    private String lastName;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    @Schema(description = "Tenant's email address", example = "john.doe@business.com", requiredMode = Schema.RequiredMode.REQUIRED)
    private String email;

    @NotBlank(message = "Phone number is required")
    @Size(min = 10, max = 15, message = "Phone number must be between 10 and 15 characters")
    @Schema(description = "Tenant's phone number", example = "+254712345678", requiredMode = Schema.RequiredMode.REQUIRED)
    private String phoneNumber;

    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    @Schema(description = "Account password", example = "SecurePass123!", requiredMode = Schema.RequiredMode.REQUIRED)
    private String password;

    // Business Details
    @NotBlank(message = "Business name is required")
    @Size(min = 2, max = 100, message = "Business name must be between 2 and 100 characters")
    @Schema(description = "Name of the business", example = "Acme Retail Store", requiredMode = Schema.RequiredMode.REQUIRED)
    private String businessName;

    @Schema(description = "Business registration number", example = "BRN-12345678")
    private String businessRegistrationNumber;

    @Schema(description = "Type of business", example = "Retail")
    private String businessType;

    @Schema(description = "Business address", example = "123 Main Street, Nairobi")
    private String businessAddress;

    @Schema(description = "Business email (if different from personal)", example = "info@acmeretail.com")
    @Email(message = "Invalid business email format")
    private String businessEmail;

    @Schema(description = "Business phone number", example = "+254201234567")
    private String businessPhone;

    @Schema(description = "Country of operation", example = "Kenya")
    private String country;

    @Schema(description = "City of operation", example = "Nairobi")
    private String city;
}
