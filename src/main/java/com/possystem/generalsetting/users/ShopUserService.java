package com.possystem.generalsetting.users;

import com.possystem.auth.pos.PosProfile;
import com.possystem.auth.pos.PosProfileRepository;
import com.possystem.common.UserStatus;
import com.possystem.generalsetting.shops.Shop;
import com.possystem.generalsetting.shops.ShopRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ShopUserService {

    private final ShopUserRepository shopUserRepository;
    private final ShopRepository shopRepository;
    private final PosProfileRepository posProfileRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public ShopUserResponse createUser(UUID tenantId, CreateShopUserRequest request) {
        // Verify shop belongs to tenant
        Shop shop = shopRepository.findByShopIdAndTenantId(request.getShopId(), tenantId)
                .orElseThrow(() -> new RuntimeException("Shop not found or access denied"));

        // Check if email/phone already exists
        if (request.getEmail() != null && shopUserRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already exists");
        }
        if (shopUserRepository.existsByPhoneNumber(request.getPhoneNumber())) {
            throw new RuntimeException("Phone number already exists");
        }

        ShopUser user = ShopUser.builder()
                .tenantId(tenantId)
                .shopId(request.getShopId())
                .email(request.getEmail())
                .phoneNumber(request.getPhoneNumber())
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .password(request.getPassword() != null ? passwordEncoder.encode(request.getPassword()) : null)
                .status(UserStatus.ACTIVE)
                .isActive(true)
                .build();

        if (request.getProfileId() != null) {
            PosProfile profile = posProfileRepository.findById(request.getProfileId())
                    .orElseThrow(() -> new RuntimeException("Profile not found"));
            user.setPosProfile(profile);
        }

        ShopUser saved = shopUserRepository.save(user);
        log.info("Created shop user: {} for shop: {}", saved.getUserId(), request.getShopId());

        return ShopUserResponse.fromEntity(saved);
    }

    @Transactional
    public ShopUserResponse updateUser(UUID tenantId, UUID userId, UpdateShopUserRequest request) {
        ShopUser user = shopUserRepository.findByUserIdAndTenantId(userId, tenantId)
                .orElseThrow(() -> new RuntimeException("User not found or access denied"));

        if (request.getEmail() != null) {
            user.setEmail(request.getEmail());
        }
        if (request.getPhoneNumber() != null) {
            user.setPhoneNumber(request.getPhoneNumber());
        }
        if (request.getFirstName() != null) {
            user.setFirstName(request.getFirstName());
        }
        if (request.getLastName() != null) {
            user.setLastName(request.getLastName());
        }
        if (request.getStatus() != null) {
            user.setStatus(request.getStatus());
        }
        if (request.getIsActive() != null) {
            user.setIsActive(request.getIsActive());
        }
        if (request.getProfileId() != null) {
            PosProfile profile = posProfileRepository.findById(request.getProfileId())
                    .orElseThrow(() -> new RuntimeException("Profile not found"));
            user.setPosProfile(profile);
        }

        ShopUser saved = shopUserRepository.save(user);
        log.info("Updated shop user: {}", saved.getUserId());

        return ShopUserResponse.fromEntity(saved);
    }

    public ShopUserResponse getUser(UUID tenantId, UUID userId) {
        ShopUser user = shopUserRepository.findByUserIdAndTenantId(userId, tenantId)
                .orElseThrow(() -> new RuntimeException("User not found or access denied"));
        return ShopUserResponse.fromEntity(user);
    }

    public List<ShopUserResponse> getUsersByTenant(UUID tenantId) {
        return shopUserRepository.findByTenantId(tenantId)
                .stream()
                .map(ShopUserResponse::fromEntity)
                .collect(Collectors.toList());
    }

    public List<ShopUserResponse> getUsersByShop(UUID tenantId, UUID shopId) {
        // Verify shop belongs to tenant
        shopRepository.findByShopIdAndTenantId(shopId, tenantId)
                .orElseThrow(() -> new RuntimeException("Shop not found or access denied"));

        return shopUserRepository.findByShopId(shopId)
                .stream()
                .map(ShopUserResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional
    public void deleteUser(UUID tenantId, UUID userId) {
        ShopUser user = shopUserRepository.findByUserIdAndTenantId(userId, tenantId)
                .orElseThrow(() -> new RuntimeException("User not found or access denied"));
        
        user.setIsActive(false);
        user.setStatus(UserStatus.DELETED);
        shopUserRepository.save(user);
        log.info("Deleted shop user: {}", userId);
    }
}
