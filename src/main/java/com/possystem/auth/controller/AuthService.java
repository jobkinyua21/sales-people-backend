package com.possystem.auth.controller;

import com.possystem.auth.security.*;
import com.possystem.auth.user.User;
import com.possystem.auth.user.UserRepository;
import com.possystem.common.ErrorCode;
import com.possystem.common.OtpService;
import com.possystem.common.UserStatus;
import com.possystem.common.UserType;
import com.possystem.role.RolePermissionRepository;
import com.possystem.role.RoleService;
import com.possystem.security.JwtService;
import com.possystem.security.UserPrincipal;
import com.possystem.shop.Shop;
import com.possystem.shop.ShopRepository;
import com.possystem.shop.UserShopAssignment;
import com.possystem.shop.UserShopAssignmentRepository;
import com.possystem.tenant.Tenant;
import com.possystem.tenant.TenantRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.context.SecurityContextHolder;
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
    private final TenantRepository tenantRepository;
    private final ShopRepository shopRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final UserShopAssignmentRepository userShopAssignmentRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final LoginAttemptService loginAttemptService;
    private final PasswordSecurityService passwordSecurityService;
    private final UserSessionService userSessionService;
    private final EmailVerificationService emailVerificationService;
    private final OtpService otpService;
    private final RoleService roleService;

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

        // Branch by user type for shop selection
        if (user.getUserType() == UserType.SYSTEM_OWNER || user.getUserType() == UserType.TENANT_ADMIN) {
            // No shop selection needed — send OTP immediately
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

        // SHOP_MANAGER or SHOP_USER — check shop assignments
        List<UserShopAssignment> assignments = userShopAssignmentRepository
                .findByUserIdAndIsActiveTrue(user.getUsrId());

        if (assignments.isEmpty()) {
            throw new IllegalArgumentException(ErrorCode.NO_SHOP_ASSIGNMENT.getDefaultMessage());
        }

        if (assignments.size() == 1) {
            // Single shop — send OTP immediately with shop context
            UUID shopId = assignments.get(0).getShopId();
            otpService.createAndSendOtp(
                    user.getUsrId(), user.getUsrEmail(), user.getUsrFirstName(),
                    user.getUserType().name(), ipAddress, shopId
            );

            return LoginResponse.builder()
                    .otpRequired(true)
                    .usrId(user.getUsrId())
                    .email(maskEmail(user.getUsrEmail()))
                    .build();
        }

        // Multiple shops — return shop list for selection, DO NOT send OTP yet
        List<ShopInfo> shopInfos = assignments.stream()
                .map(a -> {
                    Shop shop = shopRepository.findById(a.getShopId()).orElse(null);
                    if (shop == null) return null;
                    return ShopInfo.builder()
                            .shopId(shop.getId())
                            .shopName(shop.getShopName())
                            .shopCode(shop.getShopCode())
                            .shopRole(a.getShopRole())
                            .build();
                })
                .filter(java.util.Objects::nonNull)
                .toList();

        return LoginResponse.builder()
                .shopSelectionRequired(true)
                .usrId(user.getUsrId())
                .email(maskEmail(user.getUsrEmail()))
                .shops(shopInfos)
                .build();
    }

    // ==================== SHOP SELECTION (MULTI-SHOP USERS) ====================

    @Transactional
    public LoginResponse selectShop(SelectShopRequest request, HttpServletRequest httpRequest) {
        String ipAddress = getClientIp(httpRequest);

        User user = userRepository.findById(request.getUsrId())
                .orElseThrow(() -> new BadCredentialsException("User not found"));

        // Verify user has an active assignment to this shop
        userShopAssignmentRepository
                .findByUserIdAndShopIdAndIsActiveTrue(user.getUsrId(), request.getShopId())
                .orElseThrow(() -> new IllegalArgumentException(ErrorCode.INVALID_SHOP_ASSIGNMENT.getDefaultMessage()));

        // Send OTP with selected shop context
        otpService.createAndSendOtp(
                user.getUsrId(), user.getUsrEmail(), user.getUsrFirstName(),
                user.getUserType().name(), ipAddress, request.getShopId()
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

        return buildTenantAuthResponse(otp.getUsrId(), otp.getSelectedShopId(), ipAddress, userAgent);
    }

    private AuthResponse buildTenantAuthResponse(UUID usrId, UUID selectedShopId, String ipAddress, String userAgent) {
        User user = userRepository.findById(usrId)
                .orElseThrow(() -> new BadCredentialsException("User not found"));

        // Update last login info
        user.setUsrLastLogin(LocalDateTime.now());
        user.setLastLoginIp(ipAddress);
        userRepository.save(user);

        // Determine roleId and shopId for token
        UUID effectiveRoleId = user.getRoleId();
        UUID effectiveShopId = null;
        String context = "TENANT";

        if (selectedShopId != null) {
            UserShopAssignment assignment = userShopAssignmentRepository
                    .findByUserIdAndShopIdAndIsActiveTrue(usrId, selectedShopId)
                    .orElseThrow(() -> new BadCredentialsException("Shop assignment not found"));
            effectiveShopId = selectedShopId;
            effectiveRoleId = assignment.getRoleId() != null ? assignment.getRoleId() : user.getRoleId();
            context = "SHOP";
        }

        // Build principal with correct permissions
        List<String> permissionCodes = List.of();
        if (effectiveRoleId != null) {
            permissionCodes = rolePermissionRepository.findPermissionCodesByRoleId(effectiveRoleId);
        }
        UserPrincipal userPrincipal = new UserPrincipal(user, permissionCodes);
        if (effectiveShopId != null) {
            userPrincipal = userPrincipal.withShopId(effectiveShopId);
        }

        String accessToken = jwtService.generateToken(userPrincipal);
        String refreshToken = jwtService.generateRefreshToken(userPrincipal);

        // Resolve tenant info
        Tenant tenant = tenantRepository.findByUsrId(user.getUsrId()).orElse(null);
        UUID tenantId = tenant != null ? tenant.getTenantId() : user.getTenantId();
        String tenantCode = tenant != null ? tenant.getTenantCode() : null;

        // Resolve shop name if applicable
        String shopName = null;
        if (effectiveShopId != null) {
            shopName = shopRepository.findById(effectiveShopId)
                    .map(Shop::getShopName).orElse(null);
        }

        // Create session
        UserSession session = userSessionService.createSession(
                user.getUsrId(), accessToken, refreshToken,
                tenantId, effectiveShopId, context,
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
                .shopId(effectiveShopId)
                .shopName(shopName)
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

        // Determine effective role based on shop context
        UUID effectiveRoleId = user.getRoleId();
        if (session.getCurrentShopId() != null) {
            UserShopAssignment assignment = userShopAssignmentRepository
                    .findByUserIdAndShopIdAndIsActiveTrue(user.getUsrId(), session.getCurrentShopId())
                    .orElse(null);
            if (assignment != null && assignment.getRoleId() != null) {
                effectiveRoleId = assignment.getRoleId();
            }
        }

        List<String> permissionCodes = List.of();
        if (effectiveRoleId != null) {
            permissionCodes = rolePermissionRepository.findPermissionCodesByRoleId(effectiveRoleId);
        }
        UserPrincipal principal = new UserPrincipal(user, permissionCodes);

        // Preserve shop context from the session
        if (session.getCurrentShopId() != null) {
            principal = principal.withShopId(session.getCurrentShopId());
        }

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

        // Create system roles (Admin, Viewer) and assign Admin to this user
        roleService.createSystemRolesForTenant(savedTenant.getTenantId());
        UUID adminRoleId = roleService.getAdminRoleId(savedTenant.getTenantId());
        savedUser.setRoleId(adminRoleId);

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

        // Create system roles (Admin, Viewer) and assign Admin to this user
        roleService.createSystemRolesForTenant(savedTenant.getTenantId());
        UUID adminRoleId = roleService.getAdminRoleId(savedTenant.getTenantId());
        savedUser.setRoleId(adminRoleId);

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

    // ==================== SHOP CONTEXT SWITCH ====================

    @Transactional
    public AuthResponse switchShop(SwitchShopRequest request, HttpServletRequest httpRequest) {
        String ipAddress = getClientIp(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");

        UserPrincipal principal = (UserPrincipal) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();

        // Only TENANT_ADMIN can switch shop context
        if (principal.getUserType() != UserType.TENANT_ADMIN) {
            throw new IllegalArgumentException("Only tenant administrators can switch shop context");
        }

        // Validate the shop belongs to this tenant
        Shop shop = shopRepository.findByIdAndTenantId(request.getShopId(), principal.getTenantId())
                .orElseThrow(() -> new IllegalArgumentException("Shop not found in your tenant"));

        // Load the user fresh from DB
        User user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new BadCredentialsException("User not found"));

        // Build principal with permissions and override shopId
        List<String> permissionCodes = List.of();
        if (user.getRoleId() != null) {
            permissionCodes = rolePermissionRepository.findPermissionCodesByRoleId(user.getRoleId());
        }
        UserPrincipal shopPrincipal = new UserPrincipal(user, permissionCodes).withShopId(shop.getId());

        // Generate new tokens with shop context
        String accessToken = jwtService.generateToken(shopPrincipal);
        String refreshToken = jwtService.generateRefreshToken(shopPrincipal);

        // Invalidate old session from current token
        String oldToken = httpRequest.getHeader("Authorization");
        if (oldToken != null && oldToken.startsWith("Bearer ")) {
            userSessionService.invalidateSession(oldToken.substring(7));
        }

        // Create new session with shop context
        UserSession session = userSessionService.createSession(
                user.getUsrId(), accessToken, refreshToken,
                principal.getTenantId(), shop.getId(), "SHOP",
                ipAddress, userAgent);

        // Resolve tenant info
        Tenant tenant = tenantRepository.findByUsrId(user.getUsrId()).orElse(null);
        String tenantCode = tenant != null ? tenant.getTenantCode() : null;

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtService.getJwtExpiration() / 1000)
                .userType(shopPrincipal.getUserType().name())
                .usrId(user.getUsrId())
                .email(user.getUsrEmail())
                .fullName(user.getFullName())
                .tenantId(principal.getTenantId())
                .tenantCode(tenantCode)
                .sessionId(session.getId())
                .shopId(shop.getId())
                .shopName(shop.getShopName())
                .build();
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
