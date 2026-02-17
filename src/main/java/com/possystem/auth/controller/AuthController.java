package com.possystem.auth.controller;

import com.possystem.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Authentication endpoints for tenant and POS users")
public class AuthController {

    private final AuthService authService;

    // ==================== LOGIN ====================

    @Operation(
            summary = "Tenant/Admin Login",
            description = "Authenticate tenant or admin users using email and password. Sends a 4-digit OTP to the user's email for verification."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "OTP sent to email"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Invalid credentials", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Account is disabled, locked, or pending activation", content = @Content)
    })
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> loginTenant(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest) {
        LoginResponse response = authService.loginTenant(request, httpRequest);
        return ResponseEntity.ok(ApiResponse.success(response, "OTP sent to your email"));
    }

    // ==================== SHOP SELECTION (MULTI-SHOP USERS) ====================

    @Operation(
            summary = "Select Shop",
            description = "For users assigned to multiple shops: after login returns a shop list, " +
                    "the user selects a shop. This triggers an OTP to be sent. " +
                    "Then proceed to verify-otp as usual."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "OTP sent to email"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid shop selection", content = @Content)
    })
    @PostMapping("/select-shop")
    public ResponseEntity<ApiResponse<LoginResponse>> selectShop(
            @Valid @RequestBody SelectShopRequest request,
            HttpServletRequest httpRequest) {
        LoginResponse response = authService.selectShop(request, httpRequest);
        return ResponseEntity.ok(ApiResponse.success(response, "OTP sent to your email"));
    }

    // ==================== OTP VERIFICATION ====================

    @Operation(
            summary = "Verify OTP",
            description = "Verify the 4-digit OTP sent to email after login. Returns JWT tokens on successful verification."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "OTP verified, tokens issued"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid or expired OTP", content = @Content)
    })
    @PostMapping("/verify-otp")
    public ResponseEntity<ApiResponse<AuthResponse>> verifyOtp(
            @Valid @RequestBody VerifyOtpRequest request,
            HttpServletRequest httpRequest) {
        AuthResponse response = authService.verifyLoginOtp(request, httpRequest);
        return ResponseEntity.ok(ApiResponse.success(response, "OTP verified successfully"));
    }

    // ==================== TOKEN REFRESH ====================

    @Operation(
            summary = "Refresh Token",
            description = "Obtain a new access token using a valid refresh token."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Token refreshed successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Invalid or expired refresh token", content = @Content)
    })
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponse>> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        AuthResponse response = authService.refreshToken(request);
        return ResponseEntity.ok(ApiResponse.success(response, "Token refreshed successfully"));
    }

    // ==================== LOGOUT ====================

    @Operation(
            summary = "Logout",
            description = "Invalidate the current session and tokens."
    )
    @SecurityRequirement(name = "Bearer Authentication")
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(@RequestHeader("Authorization") String authorizationHeader) {
        authService.logout(authorizationHeader);
        return ResponseEntity.ok(ApiResponse.success(null, "Logged out successfully"));
    }

    // ==================== SHOP CONTEXT SWITCH ====================

    @Operation(
            summary = "Switch Shop Context",
            description = "Allows a TENANT_ADMIN to switch into a specific shop context. " +
                    "Returns new tokens with the shop embedded. All subsequent API calls will operate within that shop."
    )
    @SecurityRequirement(name = "Bearer Authentication")
    @PostMapping("/switch-shop")
    public ResponseEntity<ApiResponse<AuthResponse>> switchShop(
            @Valid @RequestBody SwitchShopRequest request,
            HttpServletRequest httpRequest) {
        AuthResponse response = authService.switchShop(request, httpRequest);
        return ResponseEntity.ok(ApiResponse.success(response, "Switched to shop: " + response.getShopName()));
    }

    // ==================== REGISTRATION ====================

    @Operation(
            summary = "Register Tenant (System Owner)",
            description = "Register a new tenant. This endpoint is for system owner use to create tenant accounts directly with ACTIVE status."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Tenant registered successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid input or email/phone already exists", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content)
    })
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
                    "Account will be created with PENDING status and requires verification."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Registration successful, pending approval"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid input or duplicate data", content = @Content)
    })
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<RegisterResponse>> selfOnboarding(@Valid @RequestBody SelfOnboardingRequest request) {
        RegisterResponse response = authService.selfOnboarding(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.created(response, "Registration successful. Your account is pending verification."));
    }

    // ==================== PASSWORD RESET (OTP FLOW) ====================

    @Operation(
            summary = "Forgot Password",
            description = "Check if email exists and send OTP for password reset. Returns user ID for subsequent OTP verification."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "OTP sent to email"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Email not found", content = @Content)
    })
    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<LoginResponse>> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request,
            HttpServletRequest httpRequest) {
        LoginResponse response = authService.forgotPassword(request, httpRequest);
        return ResponseEntity.ok(ApiResponse.success(response, "OTP sent to your email"));
    }

    @Operation(
            summary = "Verify Forgot Password OTP",
            description = "Verify the 4-digit OTP sent during forgot password flow."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "OTP verified successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid or expired OTP", content = @Content)
    })
    @PostMapping("/verify-forgot-otp")
    public ResponseEntity<ApiResponse<Void>> verifyForgotPasswordOtp(@Valid @RequestBody VerifyOtpRequest request) {
        authService.verifyForgotPasswordOtp(request.getUsrId(), request.getUsrSecret());
        return ResponseEntity.ok(ApiResponse.success(null, "OTP verified successfully"));
    }

    @Operation(
            summary = "Resend OTP",
            description = "Resend the 4-digit OTP to the user's email."
    )
    @PostMapping("/resend-phone-otp/{usrId}")
    public ResponseEntity<ApiResponse<Void>> resendOtp(
            @PathVariable UUID usrId,
            HttpServletRequest httpRequest) {
        authService.resendOtp(usrId, httpRequest);
        return ResponseEntity.ok(ApiResponse.success(null, "OTP has been resent to your email"));
    }

    @Operation(
            summary = "Update Password",
            description = "Set a new password after OTP verification during forgot password flow."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Password updated successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Password validation failed", content = @Content)
    })
    @PostMapping("/update-password")
    public ResponseEntity<ApiResponse<Void>> updatePassword(@Valid @RequestBody UpdatePasswordRequest request) {
        authService.updatePassword(request.getUsrId(), request.getUsrSecret());
        return ResponseEntity.ok(ApiResponse.success(null, "Password updated successfully. You can now login with your new password."));
    }

    // ==================== EMAIL VERIFICATION ====================

    @Operation(
            summary = "Verify Email",
            description = "Verify email address using the verification token sent via email."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Email verified successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid or expired verification token", content = @Content)
    })
    @PostMapping("/verify-email")
    public ResponseEntity<ApiResponse<Void>> verifyEmail(@Valid @RequestBody VerifyEmailRequest request) {
        authService.verifyEmail(request);
        return ResponseEntity.ok(ApiResponse.success(null, "Email verified successfully."));
    }

    @Operation(
            summary = "Resend Verification Email",
            description = "Resend the email verification link to the specified email address."
    )
    @PostMapping("/resend-verification")
    public ResponseEntity<ApiResponse<Void>> resendVerification(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.resendVerification(request);
        return ResponseEntity.ok(ApiResponse.success(null, "Verification email has been resent."));
    }
}
