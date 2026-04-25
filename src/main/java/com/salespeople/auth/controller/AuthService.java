package com.salespeople.auth.controller;

import com.salespeople.auth.security.*;
import com.salespeople.auth.user.UserTb;
import com.salespeople.auth.user.UserTbRepository;
import com.salespeople.common.OtpService;
import com.salespeople.security.JwtService;
import com.salespeople.security.UserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserTbRepository userTbRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final LoginAttemptService loginAttemptService;
    private final PasswordSecurityService passwordSecurityService;
    private final UserSessionService userSessionService;
    private final OtpService otpService;
    private final OtpVerificationRepository otpVerificationRepository;

    // ==================== LOGIN ====================

    @Transactional
    public LoginResponse login(LoginRequest request, HttpServletRequest httpRequest) {
        String ipAddress = getClientIp(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");

        UserTb user = userTbRepository.findByUserEmail(request.getEmail())
                .orElseThrow(() -> {
                    loginAttemptService.recordLoginAttempt(null, request.getEmail(),
                            false, ipAddress, userAgent, "User not found");
                    return new BadCredentialsException("Invalid email or password");
                });

        if (Boolean.TRUE.equals(user.getDeleted()) || Boolean.TRUE.equals(user.getSoftDelete())) {
            throw new BadCredentialsException("Invalid email or password");
        }

        if (loginAttemptService.isAccountLocked(user)) {
            loginAttemptService.recordLoginAttempt(user.getUserId(), user.getUserEmail(),
                    false, ipAddress, userAgent, "Account locked");
            throw new LockedException("Account is temporarily locked due to too many failed login attempts");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            loginAttemptService.incrementFailedAttempts(user);
            loginAttemptService.recordLoginAttempt(user.getUserId(), user.getUserEmail(),
                    false, ipAddress, userAgent, "Invalid password");
            throw new BadCredentialsException("Invalid email or password");
        }

        loginAttemptService.resetFailedAttempts(user);
        loginAttemptService.recordLoginAttempt(user.getUserId(), user.getUserEmail(),
                true, ipAddress, userAgent, null);

        otpService.createAndSendOtp(
                user.getUserId(), user.getUserEmail(), user.getFirstName(),
                "SALES_PERSON", ipAddress
        );

        return LoginResponse.builder()
                .otpRequired(true)
                .usrId(user.getUserId())
                .email(maskEmail(user.getUserEmail()))
                .build();
    }

    // ==================== OTP VERIFICATION ====================

    @Transactional
    public AuthResponse verifyLoginOtp(VerifyOtpRequest request, HttpServletRequest httpRequest) {
        String ipAddress = getClientIp(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");

        OtpVerification otp = otpService.verifyOtpByUserId(request.getUsrId(), request.getUsrSecret());

        UserTb user = userTbRepository.findById(otp.getUsrId())
                .orElseThrow(() -> new BadCredentialsException("User not found"));

        UserPrincipal principal = new UserPrincipal(user);

        String accessToken = jwtService.generateToken(principal);
        String refreshToken = jwtService.generateRefreshToken(principal);

        UserSession session = userSessionService.createSession(
                user.getUserId(), accessToken, refreshToken, ipAddress, userAgent);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtService.getJwtExpiration() / 1000)
                .usrId(user.getUserId())
                .email(user.getUserEmail())
                .fullName(user.getFullName())
                .staffNumber(user.getStaffNumber())
                .sessionId(session.getId())
                .build();
    }

    // ==================== TOKEN REFRESH ====================

    @Transactional
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        UserSession session = userSessionService.findByRefreshToken(request.getRefreshToken())
                .orElseThrow(() -> new BadCredentialsException("Invalid or expired refresh token"));

        if (session.getExpiresAt().isBefore(java.time.LocalDateTime.now())) {
            userSessionService.invalidateSession(session.getSessionToken());
            throw new BadCredentialsException("Session has expired. Please login again");
        }

        UserTb user = userTbRepository.findById(session.getUsrId())
                .orElseThrow(() -> new BadCredentialsException("User not found"));

        UserPrincipal principal = new UserPrincipal(user);

        String newAccessToken = jwtService.generateToken(principal);
        String newRefreshToken = jwtService.generateRefreshToken(principal);

        session.setSessionToken(newAccessToken);
        session.setRefreshToken(newRefreshToken);
        session.setLastActivityAt(java.time.LocalDateTime.now());
        session.setExpiresAt(java.time.LocalDateTime.now().plusSeconds(jwtService.getJwtExpiration() / 1000));

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

    // ==================== REGISTER ====================

    @Transactional
    public RegisterResponse registerUser(RegisterRequest request) {
        if (userTbRepository.existsByUserEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already registered");
        }

        if (request.getStaffNumber() != null && userTbRepository.existsByStaffNumber(request.getStaffNumber())) {
            throw new IllegalArgumentException("Staff number already exists");
        }

        String encodedPassword = passwordEncoder.encode(request.getPassword());

        UserTb user = UserTb.builder()
                .userEmail(request.getEmail())
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .phone(request.getPhone())
                .password(encodedPassword)
                .staffNumber(request.getStaffNumber())
                .createdBy(request.getCreatedBy())
                .deleted(false)
                .softDelete(false)
                .build();

        UserTb saved = userTbRepository.save(user);

        passwordSecurityService.savePasswordHistory(saved.getUserId(), encodedPassword);

        return RegisterResponse.builder()
                .usrId(saved.getUserId())
                .email(saved.getUserEmail())
                .fullName(saved.getFullName())
                .staffNumber(saved.getStaffNumber())
                .build();
    }

    // ==================== FORGOT PASSWORD ====================

    @Transactional
    public LoginResponse forgotPassword(ForgotPasswordRequest request, HttpServletRequest httpRequest) {
        String ipAddress = getClientIp(httpRequest);

        UserTb user = userTbRepository.findByUserEmail(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("No account found with that email"));

        otpService.createAndSendOtp(
                user.getUserId(), user.getUserEmail(), user.getFirstName(),
                "SALES_PERSON", ipAddress
        );

        return LoginResponse.builder()
                .otpRequired(true)
                .email(maskEmail(user.getUserEmail()))
                .usrId(user.getUserId())
                .build();
    }

    @Transactional
    public String verifyForgotPasswordOtp(Long usrId, String usrSecret) {
        OtpVerification otp = otpService.verifyOtpByUserId(usrId, usrSecret);

        String resetToken = java.util.UUID.randomUUID().toString();
        otp.setResetToken(resetToken);
        return resetToken;
    }

    @Transactional
    public void resendOtp(Long usrId, HttpServletRequest httpRequest) {
        String ipAddress = getClientIp(httpRequest);

        UserTb user = userTbRepository.findById(usrId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        otpService.createAndSendOtp(
                user.getUserId(), user.getUserEmail(), user.getFirstName(),
                "SALES_PERSON", ipAddress
        );
    }

    @Transactional
    public void updatePassword(Long usrId, String newPassword, String resetToken) {
        OtpVerification otp = otpVerificationRepository.findByResetTokenAndUsrId(resetToken, usrId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid or expired reset token"));

        otp.setResetToken(null);

        UserTb user = userTbRepository.findById(usrId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        String encoded = passwordEncoder.encode(newPassword);
        user.setPassword(encoded);
        userTbRepository.save(user);

        passwordSecurityService.savePasswordHistory(usrId, encoded);
    }

    // ==================== HELPERS ====================

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
