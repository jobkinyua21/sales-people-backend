package com.possystem.shop;

import com.possystem.auth.user.User;
import com.possystem.auth.user.UserRepository;
import com.possystem.common.*;
import com.possystem.security.SecurityContextUtil;
import com.possystem.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ShopUserService {

    private final UserRepository userRepository;
    private final ShopRepository shopRepository;
    private final UserShopAssignmentRepository userShopAssignmentRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    @Value("${security.password-expiry-days:90}")
    private int passwordExpiryDays;

    @Transactional
    public ShopUserResponse save(ShopUserRequest request) {
        UserPrincipal principal = SecurityContextUtil.getCurrentPrincipal();
        UUID shopId = principal.getShopId();
        UUID tenantId = principal.getTenantId();

        if (shopId == null) {
            throw new IllegalArgumentException("You are not assigned to a shop");
        }

        if (request.getId() != null) {
            return updateShopUser(request, shopId);
        }
        return createShopUser(request, shopId, tenantId);
    }

    private ShopUserResponse createShopUser(ShopUserRequest request, UUID shopId, UUID tenantId) {
        if (userRepository.existsByUsrEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already registered");
        }
        if (userRepository.existsByUsrPhoneNumber(request.getPhone())) {
            throw new IllegalArgumentException("Phone number already registered");
        }

        String randomPassword = generateRandomPassword();
        String encodedPassword = passwordEncoder.encode(randomPassword);
        String username = generateUsername(request.getEmail());
        LocalDateTime now = LocalDateTime.now();

        User user = User.builder()
                .tenantId(tenantId)
                .roleId(request.getRoleId())
                .username(username)
                .usrFirstName(request.getFirstName())
                .usrLastName(request.getLastName())
                .usrEmail(request.getEmail())
                .usrPhoneNumber(request.getPhone())
                .usrPassword(encodedPassword)
                .userType(UserType.SHOP_USER)
                .usrStatus(UserStatus.ACTIVE)
                .isActive(true)
                .emailVerified(true)
                .emailVerifiedAt(now)
                .mustChangePassword(true)
                .passwordVersion(1)
                .passwordChangedAt(now)
                .passwordExpiresAt(now.plusDays(passwordExpiryDays))
                .build();

        User savedUser = userRepository.save(user);

        // Create shop assignment
        UserShopAssignment assignment = UserShopAssignment.builder()
                .userId(savedUser.getUsrId())
                .shopId(shopId)
                .roleId(request.getRoleId())
                .shopRole(UserType.SHOP_USER)
                .build();
        userShopAssignmentRepository.save(assignment);

        // Get shop name for the email
        String shopName = shopRepository.findById(shopId)
                .map(Shop::getShopName)
                .orElse("Your Shop");

        sendUserCredentialsEmail(savedUser, randomPassword, shopName);

        return buildShopUserResponse(savedUser, shopId, shopName);
    }

    private ShopUserResponse updateShopUser(ShopUserRequest request, UUID shopId) {
        User user = userRepository.findById(request.getId())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (!userShopAssignmentRepository.existsByUserIdAndShopIdAndIsActiveTrue(user.getUsrId(), shopId)) {
            throw new IllegalArgumentException("User does not belong to your shop");
        }

        user.setUsrFirstName(request.getFirstName());
        user.setUsrLastName(request.getLastName());
        user.setUsrEmail(request.getEmail());
        user.setUsrPhoneNumber(request.getPhone());

        // Update assignment role if provided
        if (request.getRoleId() != null) {
            UserShopAssignment assignment = userShopAssignmentRepository
                    .findByUserIdAndShopIdAndIsActiveTrue(user.getUsrId(), shopId)
                    .orElse(null);
            if (assignment != null) {
                assignment.setRoleId(request.getRoleId());
                userShopAssignmentRepository.save(assignment);
            }
        }

        User savedUser = userRepository.save(user);

        String shopName = shopRepository.findById(shopId)
                .map(Shop::getShopName)
                .orElse("Your Shop");

        return buildShopUserResponse(savedUser, shopId, shopName);
    }

    public ListResponse<ShopUserResponse> fetch(FetchRequest request) {
        UserPrincipal principal = SecurityContextUtil.getCurrentPrincipal();
        UUID shopId = principal.getShopId();

        if (shopId == null) {
            throw new IllegalArgumentException("You are not assigned to a shop");
        }

        String shopName = shopRepository.findById(shopId)
                .map(Shop::getShopName)
                .orElse("Your Shop");

        if (request.getId() != null) {
            User user = userRepository.findById(request.getId())
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));
            if (!userShopAssignmentRepository.existsByUserIdAndShopIdAndIsActiveTrue(user.getUsrId(), shopId)) {
                throw new IllegalArgumentException("User does not belong to your shop");
            }
            List<ShopUserResponse> result = List.of(buildShopUserResponse(user, shopId, shopName));
            return ListResponse.of(result);
        }

        String search = request.getSearch();
        Integer limit = request.getLimit();

        if (limit == null) {
            List<User> all = userRepository.searchByShopAssignment(shopId, search);
            List<ShopUserResponse> responses = all.stream()
                    .map(u -> buildShopUserResponse(u, shopId, shopName))
                    .toList();
            return ListResponse.of(responses);
        }

        PageRequest pageRequest = PageRequest.of(request.getStart(), limit);
        Page<User> page = userRepository.searchByShopAssignment(shopId, search, pageRequest);
        Page<ShopUserResponse> responsePage = page.map(u -> buildShopUserResponse(u, shopId, shopName));
        return ListResponse.from(responsePage);
    }

    @Transactional
    public void delete(UUID id) {
        UserPrincipal principal = SecurityContextUtil.getCurrentPrincipal();
        UUID shopId = principal.getShopId();

        if (shopId == null) {
            throw new IllegalArgumentException("You are not assigned to a shop");
        }

        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        UserShopAssignment assignment = userShopAssignmentRepository
                .findByUserIdAndShopIdAndIsActiveTrue(user.getUsrId(), shopId)
                .orElseThrow(() -> new IllegalArgumentException("User does not belong to your shop"));

        if (assignment.getShopRole() == UserType.SHOP_MANAGER) {
            throw new IllegalArgumentException("Cannot delete shop manager");
        }

        // Deactivate the assignment (remove from this shop)
        assignment.setIsActive(false);
        userShopAssignmentRepository.save(assignment);

        // If no remaining active assignments, deactivate the user
        long remainingAssignments = userShopAssignmentRepository.countByUserIdAndIsActiveTrue(user.getUsrId());
        if (remainingAssignments == 0) {
            user.setIsActive(false);
            user.setUsrStatus(UserStatus.INACTIVE);
            userRepository.save(user);
        }
    }

    // ==================== HELPERS ====================

    private ShopUserResponse buildShopUserResponse(User user, UUID shopId, String shopName) {
        return ShopUserResponse.builder()
                .id(user.getUsrId())
                .username(user.getUsername())
                .firstName(user.getUsrFirstName())
                .lastName(user.getUsrLastName())
                .email(user.getUsrEmail())
                .phone(user.getUsrPhoneNumber())
                .userType(user.getUserType())
                .status(user.getUsrStatus())
                .roleId(user.getRoleId())
                .shopId(shopId)
                .shopName(shopName)
                .lastLogin(user.getUsrLastLogin())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
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

    private String generateRandomPassword() {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz23456789!@#$%";
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder(12);
        for (int i = 0; i < 12; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }

    private void sendUserCredentialsEmail(User user, String password, String shopName) {
        String subject = "Your POS Account - " + shopName;
        String body = String.format("""
                Hello %s,

                You have been added as a user at %s.

                Here are your login credentials:
                Email: %s
                Temporary Password: %s

                Please login and change your password immediately.

                Regards,
                POS System
                """,
                user.getUsrFirstName(),
                shopName,
                user.getUsrEmail(),
                password
        );

        try {
            emailService.sendSimpleEmail(user.getUsrEmail(), subject, body);
        } catch (Exception e) {
            log.error("Failed to send credentials email to user: {}", user.getUsrEmail(), e);
        }
    }

}
