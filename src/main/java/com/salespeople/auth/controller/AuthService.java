package com.salespeople.auth.controller;

import com.salespeople.auth.security.*;
import com.salespeople.auth.user.User;
import com.salespeople.auth.user.UserRepository;
import com.salespeople.common.ErrorCode;
import com.salespeople.common.OtpService;
import com.salespeople.common.UserStatus;
import com.salespeople.common.UserType;
import com.salespeople.role.RolePermissionRepository;
import com.salespeople.role.RoleService;
import com.salespeople.security.JwtService;
import com.salespeople.security.UserPrincipal;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final LoginAttemptService loginAttemptService;
    private final PasswordSecurityService passwordSecurityService;
    private final UserSessionService userSessionService;
    private final EmailVerificationService emailVerificationService;
    private final OtpService otpService;
    private final OtpVerificationRepository otpVerificationRepository;
    private final RoleService roleService;

    @Value("${security.password-expiry-days:90}")
    private int passwordExpiryDays;

    // ==================== LOGIN (OTP FLOW) ====================

    @Transactional
    public LoginResponse login(LoginRequest request, HttpServletRequest httpRequest) {
        String ipAddress = getClientIp(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");

        User user = userRepository.findByUsrEmail(request.getEmail())
                .orElseThrow(() -> {
                    loginAttemptService.recordLoginAttempt(null, null, request.getEmail(),
                            false, ipAddress, userAgent, "User not found");
                    return new BadCredentialsException("Invalid email or password");
                });

        // Check if account is locked
        if (loginAttemptService.isAccountLocked(user)) {
            loginAttemptService.recordLoginAttempt(user.getUsrId(), user.getUsername(),
                    user.getUsrEmail(), false, ipAddress, userAgent, "Account locked");
            throw new LockedException("Account is temporarily locked due to too many failed login attempts");
        }

        validateUserStatus(user.getUsrStatus(), user.getIsActive());



        // Validate password
        if (!passwordEncoder.matches(request.getPassword(), user.getUsrPassword())) {
            loginAttemptService.incrementFailedAttempts(user);
            loginAttemptService.recordLoginAttempt(user.getUsrId(), user.getUsername(),
                    user.getUsrEmail(), false, ipAddress, userAgent, "Invalid password");
            throw new BadCredentialsException("Invalid email or password");
        }

        // Successful password validation - reset failed attempts
        loginAttemptService.resetFailedAttempts(user);
        loginAttemptService.recordLoginAttempt(user.getUsrId(), user.getUsername(),
                user.getUsrEmail(), true, ipAddress, userAgent, null);

        // Send OTP
        otpService.createAndSendOtp(
                user.getUsrId(), user.getUsrEmail(), user.getUsrFirstName(),
                user.getUserType().name(), ipAddress
        );


        return LoginResponse.builder()
                .otpRequired(true)
                .usrId(user.getUsrId())
                .email(maskEmail(user.getUsrEmail()))
                .build();
    }

    // ==================== OTP VERIFICATION ====================

    @Transactional
    public AuthResponse verifyLoginOtp(VerifyOtpRequest request, HttpServletRequest httpRequest) {
        String ipAddress = getClientIp(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");

        OtpVerification otp = otpService.verifyOtpByUserId(request.getUsrId(), request.getUsrSecret());

        return buildAuthResponse(otp.getUsrId(), ipAddress, userAgent);
    }

    private AuthResponse buildAuthResponse(UUID usrId, String ipAddress, String userAgent) {
        User user = userRepository.findById(usrId)
                .orElseThrow(() -> new BadCredentialsException("User not found"));

        // Update last login info
        user.setUsrLastLogin(LocalDateTime.now());
        user.setLastLoginIp(ipAddress);
        userRepository.save(user);

        // Build principal with permissions
        List<String> permissionCodes = List.of();
        if (user.getRoleId() != null) {
            permissionCodes = rolePermissionRepository.findPermissionCodesByRoleId(user.getRoleId());
        }
        UserPrincipal userPrincipal = new UserPrincipal(user, permissionCodes);

        String accessToken = jwtService.generateToken(userPrincipal);
        String refreshToken = jwtService.generateRefreshToken(userPrincipal);

        // Create session
        UserSession session = userSessionService.createSession(
                user.getUsrId(), accessToken, refreshToken,
                ipAddress, userAgent);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtService.getJwtExpiration() / 1000)
                .userType(userPrincipal.getUserType().name())
                .usrId(user.getUsrId())
                .email(user.getUsrEmail())
                .fullName(user.getFullName())
                .sessionId(session.getId())
                .mustChangePassword(user.getMustChangePassword())
                .build();
    }

    // ==================== TOKEN REFRESH ====================

    @Transactional
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        UserSession session = userSessionService.findByRefreshToken(request.getRefreshToken())
                .orElseThrow(() -> new BadCredentialsException("Invalid or expired refresh token"));

        if (session.getExpiresAt().isBefore(LocalDateTime.now())) {
            userSessionService.invalidateSession(session.getSessionToken());
            throw new BadCredentialsException("Session has expired. Please login again");
        }

        String username = jwtService.extractUsername(session.getSessionToken());

        User user = userRepository.findByUsrEmail(username)
                .orElseThrow(() -> new BadCredentialsException("User not found"));

        List<String> permissionCodes = List.of();
        if (user.getRoleId() != null) {
            permissionCodes = rolePermissionRepository.findPermissionCodesByRoleId(user.getRoleId());
        }
        UserPrincipal principal = new UserPrincipal(user, permissionCodes);

        String newAccessToken = jwtService.generateToken(principal);
        String newRefreshToken = jwtService.generateRefreshToken(principal);

        // Update session with new tokens
        session.setSessionToken(newAccessToken);
        session.setRefreshToken(newRefreshToken);
        session.setLastActivityAt(LocalDateTime.now());
        session.setExpiresAt(LocalDateTime.now().plusSeconds(jwtService.getJwtExpiration() / 1000));

        return AuthResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtService.getJwtExpiration() / 1000)
                .build();
    }

    // ==================== LOGOUT ====================

    @Transactional
    public void logout(String authorizationHeader) {
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            String token = authorizationHeader.substring(7);
            userSessionService.invalidateSession(token);
        }
    }

    // ==================== REGISTER USER (Admin creates sales people, or first admin) ====================

    @Transactional
    public RegisterResponse registerUser(RegisterRequest request) {
        validateEmailNotExists(request.getEmail());
        validatePhoneNotExists(request.getPhoneNumber());

        UserType userType = UserType.SALES_PERSON;
        if (request.getUserType() != null && request.getUserType().equalsIgnoreCase("ADMIN")) {
            userType = UserType.ADMIN;
        }

        String username = generateUsername(request.getEmail());
        String encodedPassword = passwordEncoder.encode(request.getPassword());

        User user = User.builder()
                .username(username)
                .usrFirstName(request.getFirstName())
                .usrLastName(request.getLastName())
                .usrEmail(request.getEmail())
                .usrPhoneNumber(request.getPhoneNumber())
                .usrPassword(encodedPassword)
                .userType(userType)
                .usrStatus(UserStatus.ACTIVE)
                .isActive(true)
                .emailVerified(true)
                .emailVerifiedAt(LocalDateTime.now())
                .passwordVersion(1)
                .passwordChangedAt(LocalDateTime.now())
                .passwordExpiresAt(LocalDateTime.now().plusDays(passwordExpiryDays))
                .build();

        User savedUser = userRepository.save(user);

        // Save password to history
        passwordSecurityService.savePasswordHistory(savedUser.getUsrId(), encodedPassword);

        return RegisterResponse.builder()
                .usrId(savedUser.getUsrId())
                .email(savedUser.getUsrEmail())
                .fullName(savedUser.getFullName())
                .userType(savedUser.getUserType().name())
                .status(savedUser.getUsrStatus().name())
                .build();
    }

    // ==================== FORGOT PASSWORD (OTP FLOW) ====================

    @Transactional
    public LoginResponse forgotPassword(ForgotPasswordRequest request, HttpServletRequest httpRequest) {
        String ipAddress = getClientIp(httpRequest);

        User user = userRepository.findByUsrEmail(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("No account found with that email"));

        otpService.createAndSendOtp(
                user.getUsrId(),
                user.getUsrEmail(),
                user.getUsrFirstName(),
                user.getUserType().name(),
                ipAddress
        );

        return LoginResponse.builder()
                .otpRequired(true)
                .email(maskEmail(user.getUsrEmail()))
                .usrId(user.getUsrId())
                .build();
    }

    @Transactional
    public String verifyForgotPasswordOtp(UUID usrId, String usrSecret) {
        OtpVerification otp = otpService.verifyOtpByUserId(usrId, usrSecret);

        // Generate a one-time reset token to prove OTP was verified
        String resetToken = UUID.randomUUID().toString();
        otp.setResetToken(resetToken);
        return resetToken;
    }

    @Transactional
    public void resendOtp(UUID usrId, HttpServletRequest httpRequest) {
        String ipAddress = getClientIp(httpRequest);

        User user = userRepository.findById(usrId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        otpService.createAndSendOtp(
                user.getUsrId(),
                user.getUsrEmail(),
                user.getUsrFirstName(),
                user.getUserType().name(),
                ipAddress
        );
    }

    @Transactional
    public void updatePassword(UUID usrId, String newPassword, String resetToken) {
        // Validate the reset token proves OTP was verified
        OtpVerification otp = otpVerificationRepository.findByResetTokenAndUsrId(resetToken, usrId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid or expired reset token"));

        // Invalidate the reset token so it can't be reused
        otp.setResetToken(null);

        User user = userRepository.findById(usrId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        user.setUsrPassword(passwordEncoder.encode(newPassword));
        user.setPasswordVersion(user.getPasswordVersion() + 1);
        user.setPasswordChangedAt(LocalDateTime.now());
        user.setPasswordExpiresAt(LocalDateTime.now().plusDays(passwordExpiryDays));
        user.setMustChangePassword(false);
        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
        userRepository.save(user);
    }

    // ==================== EMAIL VERIFICATION ====================

    @Transactional
    public void verifyEmail(VerifyEmailRequest request) {
        emailVerificationService.verifyEmail(request.getToken());
    }

    @Transactional
    public void resendVerification(ForgotPasswordRequest request) {
        emailVerificationService.resendVerificationToken(request.getEmail());
    }

    // ==================== HELPER METHODS ====================

    private void validateEmailNotExists(String email) {
        if (userRepository.existsByUsrEmail(email)) {
            throw new IllegalArgumentException("Email already registered");
        }
    }

    private void validatePhoneNotExists(String phoneNumber) {
        if (userRepository.existsByUsrPhoneNumber(phoneNumber)) {
            throw new IllegalArgumentException("Phone number already registered");
        }
    }

    private void validateUserStatus(UserStatus status, Boolean isActive) {
        if (!Boolean.TRUE.equals(isActive)) {
            throw new DisabledException("Account is deactivated");
        }

        switch (status) {
            case PENDING -> throw new DisabledException("Account is pending activation");
            case INACTIVE -> throw new DisabledException("Account is inactive");
            case SUSPENDED -> throw new LockedException("Account is suspended");
            case DELETED -> throw new DisabledException("Account has been deleted");
            case ACTIVE -> { }
        }
    }

    private String generateUsername(String email) {
        String base = email.split("@")[0].replaceAll("[^a-zA-Z0-9]", "");
        String username = base;
        int counter = 1;
        while (userRepository.existsByUsername(username)) {
            username = base + counter;
            counter++;
        }
        return username;
    }

    private String maskEmail(String email) {
        if (email == null) return null;
        int atIndex = email.indexOf('@');
        if (atIndex <= 1) return email;
        return email.charAt(0) + "***" + email.substring(atIndex);
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
