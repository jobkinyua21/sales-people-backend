package com.possystem.auth.controller;

import com.possystem.auth.security.*;
import com.possystem.auth.user.User;
import com.possystem.auth.user.UserRepository;
import com.possystem.common.OtpService;
import com.possystem.common.UserStatus;
import com.possystem.common.UserType;
import com.possystem.security.JwtService;
import com.possystem.security.UserPrincipal;
import com.possystem.tenant.Tenant;
import com.possystem.tenant.TenantRepository;
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
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final TenantRepository tenantRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final LoginAttemptService loginAttemptService;
    private final PasswordSecurityService passwordSecurityService;
    private final UserSessionService userSessionService;
    private final EmailVerificationService emailVerificationService;
    private final OtpService otpService;

    @Value("${security.password-expiry-days:90}")
    private int passwordExpiryDays;

    // ==================== LOGIN FLOWS (OTP) ====================

    @Transactional
    public LoginResponse loginTenant(LoginRequest request, HttpServletRequest httpRequest) {
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

        // Generate and send OTP instead of tokens
        otpService.createAndSendOtp(
                user.getUsrId(),
                user.getUsrEmail(),
                user.getUsrFirstName(),
                user.getUserType().name(),
                ipAddress
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

        return buildTenantAuthResponse(otp.getUsrId(), ipAddress, userAgent);
    }

    private AuthResponse buildTenantAuthResponse(UUID usrId, String ipAddress, String userAgent) {
        User user = userRepository.findById(usrId)
                .orElseThrow(() -> new BadCredentialsException("User not found"));

        // Update last login info
        user.setUsrLastLogin(LocalDateTime.now());
        user.setLastLoginIp(ipAddress);
        userRepository.save(user);

        // Generate tokens
        UserPrincipal userPrincipal = new UserPrincipal(user);
        String accessToken = jwtService.generateToken(userPrincipal);
        String refreshToken = jwtService.generateRefreshToken(userPrincipal);

        // Resolve tenant info
        Tenant tenant = tenantRepository.findByUsrId(user.getUsrId()).orElse(null);
        UUID tenantId = tenant != null ? tenant.getTenantId() : user.getTenantId();
        String tenantCode = tenant != null ? tenant.getTenantCode() : null;

        // Create session
        UserSession session = userSessionService.createSession(
                user.getUsrId(), accessToken, refreshToken,
                tenantId, null, "TENANT",
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
                .tenantId(tenantId)
                .tenantCode(tenantCode)
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
        UserPrincipal principal = new UserPrincipal(user);
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

    // ==================== REGISTRATION FLOWS ====================

    @Transactional
    public RegisterResponse registerTenant(RegisterTenantRequest request) {
        validateEmailNotExists(request.getEmail());
        validatePhoneNotExists(request.getPhoneNumber());

        String username = generateUsername(request.getEmail());
        String encodedPassword = passwordEncoder.encode(request.getPassword());

        User user = User.builder()
                .username(username)
                .usrFirstName(request.getFirstName())
                .usrLastName(request.getLastName())
                .usrEmail(request.getEmail())
                .usrPhoneNumber(request.getPhoneNumber())
                .usrPassword(encodedPassword)
                .userType(UserType.TENANT_ADMIN)
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

        // Create tenant with auto-generated tenantCode
        String tenantCode = generateTenantCode();
        Tenant tenant = Tenant.builder()
                .tenantCode(tenantCode)
                .usrId(savedUser.getUsrId())
                .businessName(savedUser.getFullName() + "'s Business")
                .email(savedUser.getUsrEmail())
                .phone(savedUser.getUsrPhoneNumber())
                .status(UserStatus.ACTIVE)
                .isActive(true)
                .isVerified(true)
                .build();

        Tenant savedTenant = tenantRepository.save(tenant);

        // Link user to tenant
        savedUser.setTenantId(savedTenant.getTenantId());
        userRepository.save(savedUser);

        return RegisterResponse.builder()
                .usrId(savedUser.getUsrId())
                .tenantId(savedTenant.getTenantId())
                .email(savedUser.getUsrEmail())
                .fullName(savedUser.getFullName())
                .userType(savedUser.getUserType().name())
                .status(savedUser.getUsrStatus().name())
                .tenantCode(tenantCode)
                .build();
    }

    @Transactional
    public RegisterResponse selfOnboarding(SelfOnboardingRequest request) {
        validateEmailNotExists(request.getEmail());
        validatePhoneNotExists(request.getPhoneNumber());

        String username = generateUsername(request.getEmail());
        String encodedPassword = passwordEncoder.encode(request.getPassword());

        User user = User.builder()
                .username(username)
                .usrFirstName(request.getFirstName())
                .usrLastName(request.getLastName())
                .usrEmail(request.getEmail())
                .usrPhoneNumber(request.getPhoneNumber())
                .usrPassword(encodedPassword)
                .userType(UserType.TENANT_ADMIN)
                .usrStatus(UserStatus.PENDING)
                .isActive(true)
                .emailVerified(false)
                .passwordVersion(1)
                .passwordChangedAt(LocalDateTime.now())
                .passwordExpiresAt(LocalDateTime.now().plusDays(passwordExpiryDays))
                .build();

        User savedUser = userRepository.save(user);

        // Save password to history
        passwordSecurityService.savePasswordHistory(savedUser.getUsrId(), encodedPassword);

        // Generate email verification token
        emailVerificationService.generateVerificationToken(savedUser);

        // Create tenant with auto-generated tenantCode
        String tenantCode = generateTenantCode();
        Tenant tenant = Tenant.builder()
                .tenantCode(tenantCode)
                .usrId(savedUser.getUsrId())
                .businessName(request.getBusinessName())
                .businessType(request.getBusinessType())
                .businessAddress(request.getBusinessAddress())
                .businessEmail(request.getBusinessEmail())
                .businessPhone(request.getBusinessPhone())
                .email(request.getEmail())
                .phone(request.getPhoneNumber())
                .country(request.getCountry())
                .city(request.getCity())
                .status(UserStatus.PENDING)
                .isActive(true)
                .isVerified(false)
                .build();

        Tenant savedTenant = tenantRepository.save(tenant);

        // Link user to tenant
        savedUser.setTenantId(savedTenant.getTenantId());
        userRepository.save(savedUser);

        return RegisterResponse.builder()
                .usrId(savedUser.getUsrId())
                .tenantId(savedTenant.getTenantId())
                .email(savedUser.getUsrEmail())
                .fullName(savedUser.getFullName())
                .userType(savedUser.getUserType().name())
                .status(savedUser.getUsrStatus().name())
                .tenantCode(tenantCode)
                .businessName(savedTenant.getBusinessName())
                .build();
    }

    // ==================== FORGOT PASSWORD (OTP FLOW) ====================

    @Transactional
    public LoginResponse forgotPassword(ForgotPasswordRequest request, HttpServletRequest httpRequest) {
        String ipAddress = getClientIp(httpRequest);

        User user = userRepository.findByUsrEmail(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("No account found with that email"));

        // Send OTP to user's email
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
    public void verifyForgotPasswordOtp(UUID usrId, String usrSecret) {
        otpService.verifyOtpByUserId(usrId, usrSecret);
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
    public void updatePassword(UUID usrId, String usrSecret) {
        User user = userRepository.findById(usrId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // Update password
        user.setUsrPassword(passwordEncoder.encode(usrSecret));
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

    private String generateTenantCode() {
        String code;
        do {
            code = "TNT-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        } while (tenantRepository.existsByTenantCode(code));
        return code;
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
