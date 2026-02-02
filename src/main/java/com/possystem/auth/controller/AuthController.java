package com.possystem.auth.controller;

import com.possystem.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Authentication endpoints for tenant and POS users")
public class AuthController {

    private final AuthService authService;

    @Operation(
            summary = "Tenant/Admin Login",
            description = "Authenticate tenant or admin users using email and password. Returns JWT tokens for accessing protected resources."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Successfully authenticated"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Invalid credentials",
                    content = @Content
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "Account is disabled, locked, or pending activation",
                    content = @Content
            )
    })
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> loginTenant(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.loginTenant(request);
        return ResponseEntity.ok(ApiResponse.success(response, "Login successful"));
    }

    @Operation(
            summary = "POS User Login",
            description = "Authenticate POS users (shop staff) using username (email or phone), password, and shop code. " +
                    "The shop code is a unique identifier for the shop that is easy to remember."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Successfully authenticated"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Invalid credentials or shop code",
                    content = @Content
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "Account or shop is disabled, locked, or pending activation",
                    content = @Content
            )
    })
    @PostMapping("/pos/login")
    public ResponseEntity<ApiResponse<AuthResponse>> loginPosUser(@Valid @RequestBody PosLoginRequest request) {
        AuthResponse response = authService.loginPosUser(request);
        return ResponseEntity.ok(ApiResponse.success(response, "Login successful"));
    }

    @Operation(
            summary = "Register Tenant (Admin)",
            description = "Register a new tenant. This endpoint is for admin use to create tenant accounts directly with ACTIVE status."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201",
                    description = "Tenant registered successfully"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Invalid input or email/phone already exists",
                    content = @Content
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - Admin authentication required",
                    content = @Content
            )
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/admin/register-tenant")
    public ResponseEntity<ApiResponse<RegisterResponse>> registerTenant(@Valid @RequestBody RegisterTenantRequest request) {
        RegisterResponse response = authService.registerTenant(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.created(response, "Tenant registered successfully"));
    }

    @Operation(
            summary = "Self Onboarding",
            description = "Self-registration for new tenants. Requires personal and business details. " +
                    "Account will be created with PENDING status and requires admin approval."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201",
                    description = "Registration successful, pending approval"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Invalid input, email/phone already exists, or business registration number already exists",
                    content = @Content
            )
    })
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<RegisterResponse>> selfOnboarding(@Valid @RequestBody SelfOnboardingRequest request) {
        RegisterResponse response = authService.selfOnboarding(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.created(response, "Registration successful. Your account is pending verification."));
    }
}
