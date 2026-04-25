package com.salespeople.auth.controller;

import com.salespeople.common.ApiResponse;
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

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Authentication endpoints for admin and sales people")
public class AuthController {

    private final AuthService authService;

    // ==================== LOGIN ====================

    @Operation(
            summary = "Login",
            description = "Authenticate using email and password. Sends a 4-digit OTP to the user's email for verification."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "OTP sent to email"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Invalid credentials", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Account is disabled, locked, or pending activation", content = @Content)
    })
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest) {
        LoginResponse response = authService.login(request, httpRequest);
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

    // ==================== REGISTER ====================

    @Operation(
            summary = "Register User",
            description = "Register a new user (admin or sales person). Admin users can create sales people accounts."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "User registered successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid input or email/phone already exists", content = @Content)
    })
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<RegisterResponse>> register(@Valid @RequestBody RegisterRequest request) {
        RegisterResponse response = authService.registerUser(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.created(response, "User registered successfully"));
    }

    // ==================== PASSWORD RESET (OTP FLOW) ====================

    @Operation(
            summary = "Forgot Password",
            description = "Send OTP for password reset. Returns user ID for subsequent OTP verification."
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
    public ResponseEntity<ApiResponse<Map<String, String>>> verifyForgotPasswordOtp(@Valid @RequestBody VerifyOtpRequest request) {
        String resetToken = authService.verifyForgotPasswordOtp(request.getUsrId(), request.getUsrSecret());
        Map<String, String> data = Map.of(
                "resetToken", resetToken,
                "usrId", String.valueOf(request.getUsrId())
        );
        return ResponseEntity.ok(ApiResponse.success(data, "OTP verified successfully"));
    }

    @Operation(
            summary = "Resend OTP",
            description = "Resend the 4-digit OTP to the user's email."
    )
    @PostMapping("/resend-otp/{usrId}")
    public ResponseEntity<ApiResponse<Void>> resendOtp(
            @PathVariable Long usrId,
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
        authService.updatePassword(request.getUsrId(), request.getUsrSecret(), request.getResetToken());
        return ResponseEntity.ok(ApiResponse.success(null, "Password updated successfully. You can now login with your new password."));
    }

}
