package com.possystem.auth.controller;

import com.possystem.auth.pos.PosUser;
import com.possystem.auth.pos.PosUserRepository;
import com.possystem.auth.user.User;
import com.possystem.auth.user.UserRepository;
import com.possystem.common.UserStatus;
import com.possystem.common.UserType;
import com.possystem.generalsetting.shops.Shop;
import com.possystem.generalsetting.shops.ShopRepository;
import com.possystem.security.JwtService;
import com.possystem.security.PosUserPrincipal;
import com.possystem.security.UserPrincipal;
import com.possystem.tenant.Tenant;
import com.possystem.tenant.TenantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PosUserRepository posUserRepository;
    private final ShopRepository shopRepository;
    private final TenantRepository tenantRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public AuthResponse loginTenant(LoginRequest request) {
        User user = userRepository.findByUsrEmail(request.getEmail())
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));

        validateUserStatus(user.getUsrStatus(), user.getIsActive());

        if (!passwordEncoder.matches(request.getPassword(), user.getUsrPassword())) {
            throw new BadCredentialsException("Invalid email or password");
        }

        user.setUsrLastLogin(LocalDateTime.now());
        userRepository.save(user);

        UserPrincipal userPrincipal = new UserPrincipal(user);
        String accessToken = jwtService.generateToken(userPrincipal);
        String refreshToken = jwtService.generateRefreshToken(userPrincipal);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtService.getJwtExpiration() / 1000)
                .userType(userPrincipal.getUserType().name())
                .userId(user.getUsrId())
                .email(user.getUsrEmail())
                .fullName(user.getFullName())
                .build();
    }

    @Transactional
    public AuthResponse loginPosUser(PosLoginRequest request) {
        Shop shop = shopRepository.findByShopCode(request.getShopCode())
                .orElseThrow(() -> new BadCredentialsException("Invalid shop code"));

        if (!shop.getIsActive() || shop.getStatus() != UserStatus.ACTIVE) {
            throw new DisabledException("Shop is not active");
        }

        PosUser posUser = posUserRepository.findByEmailAndShopId(request.getUsername(), shop.getShopId())
                .or(() -> posUserRepository.findByPhoneNumberAndShopId(request.getUsername(), shop.getShopId()))
                .orElseThrow(() -> new BadCredentialsException("Invalid username or password"));

        validateUserStatus(posUser.getStatus(), posUser.getIsActive());

        if (!passwordEncoder.matches(request.getPassword(), posUser.getPassword())) {
            throw new BadCredentialsException("Invalid username or password");
        }

        posUser.setLastLogin(LocalDateTime.now());
        posUserRepository.save(posUser);

        PosUserPrincipal posUserPrincipal = new PosUserPrincipal(posUser);
        String accessToken = jwtService.generateToken(posUserPrincipal);
        String refreshToken = jwtService.generateRefreshToken(posUserPrincipal);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtService.getJwtExpiration() / 1000)
                .userType(posUserPrincipal.getUserType().name())
                .userId(posUser.getUserId())
                .email(posUser.getEmail())
                .fullName(posUser.getFullName())
                .tenantId(posUser.getTenantId())
                .shopId(shop.getShopId())
                .shopName(shop.getShopName())
                .build();
    }

    @Transactional
    public RegisterResponse registerTenant(RegisterTenantRequest request) {
        validateEmailNotExists(request.getEmail());
        validatePhoneNotExists(request.getPhoneNumber());

        User user = User.builder()
                .usrFirstName(request.getFirstName())
                .usrLastName(request.getLastName())
                .usrEmail(request.getEmail())
                .usrPhoneNumber(request.getPhoneNumber())
                .usrPassword(passwordEncoder.encode(request.getPassword()))
                .userType(UserType.TENANT)
                .usrStatus(UserStatus.ACTIVE)
                .isActive(true)
                .build();

        User savedUser = userRepository.save(user);

        return RegisterResponse.builder()
                .userId(savedUser.getUsrId())
                .email(savedUser.getUsrEmail())
                .fullName(savedUser.getFullName())
                .userType(savedUser.getUserType().name())
                .status(savedUser.getUsrStatus().name())
                .message("Tenant registered successfully")
                .build();
    }

    @Transactional
    public RegisterResponse selfOnboarding(SelfOnboardingRequest request) {
        validateEmailNotExists(request.getEmail());
        validatePhoneNotExists(request.getPhoneNumber());

        if (request.getBusinessRegistrationNumber() != null &&
                tenantRepository.existsByBusinessRegistrationNumber(request.getBusinessRegistrationNumber())) {
            throw new IllegalArgumentException("Business registration number already exists");
        }

        User user = User.builder()
                .usrFirstName(request.getFirstName())
                .usrLastName(request.getLastName())
                .usrEmail(request.getEmail())
                .usrPhoneNumber(request.getPhoneNumber())
                .usrPassword(passwordEncoder.encode(request.getPassword()))
                .userType(UserType.TENANT)
                .usrStatus(UserStatus.PENDING)
                .isActive(true)
                .build();

        User savedUser = userRepository.save(user);

        Tenant tenant = Tenant.builder()
                .userId(savedUser.getUsrId())
                .businessName(request.getBusinessName())
                .businessRegistrationNumber(request.getBusinessRegistrationNumber())
                .businessType(request.getBusinessType())
                .businessAddress(request.getBusinessAddress())
                .businessEmail(request.getBusinessEmail())
                .businessPhone(request.getBusinessPhone())
                .country(request.getCountry())
                .city(request.getCity())
                .status(UserStatus.PENDING)
                .isActive(true)
                .isVerified(false)
                .build();

        Tenant savedTenant = tenantRepository.save(tenant);

        return RegisterResponse.builder()
                .userId(savedUser.getUsrId())
                .tenantId(savedTenant.getTenantId())
                .email(savedUser.getUsrEmail())
                .fullName(savedUser.getFullName())
                .userType(savedUser.getUserType().name())
                .status(savedUser.getUsrStatus().name())
                .businessName(savedTenant.getBusinessName())
                .message("Registration successful. Your account is pending verification. You will receive an email once approved.")
                .build();
    }

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
}
