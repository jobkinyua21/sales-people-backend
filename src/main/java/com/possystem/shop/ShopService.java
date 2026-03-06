package com.possystem.shop;

import com.possystem.auth.user.User;
import com.possystem.auth.user.UserRepository;
import com.possystem.businesstype.BusinessType;
import com.possystem.businesstype.BusinessTypeModule;
import com.possystem.businesstype.BusinessTypeModuleRepository;
import com.possystem.businesstype.BusinessTypeRepository;
import com.possystem.common.*;
import com.possystem.module.AdditionalModule;
import com.possystem.module.AdditionalModuleRepository;
import com.possystem.module.enums.ModuleStatus;
import com.possystem.role.RoleService;
import com.possystem.shop.enums.ShopStatus;
import com.possystem.shop.enums.SubscriptionStatus;
import com.possystem.security.SecurityContextUtil;
import com.possystem.subscription.SubscriptionPlan;
import com.possystem.subscription.SubscriptionPlanRepository;
import com.possystem.subscription.SubscriptionPlanStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
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
public class ShopService {

    private final ShopRepository shopRepository;
    private final ShopSubscriptionRepository shopSubscriptionRepository;
    private final ShopAdditionalModuleRepository shopAdditionalModuleRepository;
    private final AdditionalModuleRepository additionalModuleRepository;
    private final SubscriptionPlanRepository subscriptionPlanRepository;
    private final UserRepository userRepository;
    private final UserShopAssignmentRepository userShopAssignmentRepository;
    private final BusinessTypeRepository businessTypeRepository;
    private final BusinessTypeModuleRepository businessTypeModuleRepository;
    private final RoleService roleService;
    private final ModelMapper modelMapper;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    @Value("${shop.trial-days:14}")
    private int trialDays;

    @Value("${shop.grace-period-days:7}")
    private int gracePeriodDays;

    @Value("${security.password-expiry-days:90}")
    private int passwordExpiryDays;

    @Transactional
    public ShopResponse save(ShopRequest request) {
        UUID tenantId = SecurityContextUtil.getCurrentTenantId();

        if (request.getId() != null) {
            return updateShop(request, tenantId);
        }
        return createShop(request, tenantId);
    }

    private ShopResponse createShop(ShopRequest request, UUID tenantId) {
        // Validate required fields on create
        if (request.getShopName() == null || request.getShopName().isBlank()) {
            throw new IllegalArgumentException("Shop name is required");
        }
        if (request.getSubscriptionPlanId() == null) {
            throw new IllegalArgumentException("Subscription plan is required");
        }
        if (request.getBillingCycle() == null) {
            throw new IllegalArgumentException("Billing cycle is required");
        }
        if (request.getPaymentMode() == null) {
            throw new IllegalArgumentException("Payment mode is required");
        }

        // Validate subscription plan
        SubscriptionPlan plan = subscriptionPlanRepository.findById(request.getSubscriptionPlanId())
                .orElseThrow(() -> new IllegalArgumentException("Subscription plan not found"));

        if (plan.getStatus() != SubscriptionPlanStatus.ACTIVE) {
            throw new IllegalArgumentException("Subscription plan is not active");
        }

        boolean hasManager = request.getManagerEmail() != null && !request.getManagerEmail().isBlank();

        // Validate manager fields only if manager details are provided
        boolean existingManager = false;
        if (hasManager) {
            User existingUser = userRepository.findByUsrEmail(request.getManagerEmail()).orElse(null);
            if (existingUser != null) {
                // Existing user — validate they belong to this tenant
                if (!tenantId.equals(existingUser.getTenantId())) {
                    throw new IllegalArgumentException("Manager email belongs to a different tenant");
                }
                existingManager = true;
            } else {
                // New user — validate required fields
                if (request.getManagerPhone() == null || request.getManagerPhone().isBlank()) {
                    throw new IllegalArgumentException("Manager phone is required");
                }
                if (request.getManagerFirstName() == null || request.getManagerFirstName().isBlank()) {
                    throw new IllegalArgumentException("Manager first name is required");
                }
                if (request.getManagerLastName() == null || request.getManagerLastName().isBlank()) {
                    throw new IllegalArgumentException("Manager last name is required");
                }
                if (userRepository.existsByUsrPhoneNumber(request.getManagerPhone())) {
                    throw new IllegalArgumentException("Manager phone number already registered");
                }
            }
        }

        // Validate business type if provided
        if (request.getBusinessTypeId() != null) {
            businessTypeRepository.findByIdAndIsActiveTrue(request.getBusinessTypeId())
                    .orElseThrow(() -> new IllegalArgumentException("Business type not found"));
        }

        // Create shop
        Shop shop = modelMapper.map(request, Shop.class);
        shop.setTenantId(tenantId);
        shop.setShopCode(generateShopCode());
        shop.setStatus(ShopStatus.ACTIVE);

        Shop savedShop = shopRepository.save(shop);

        // Create shop subscription
        LocalDateTime now = LocalDateTime.now();
        ShopSubscription subscription = ShopSubscription.builder()
                .shopId(savedShop.getId())
                .subscriptionPlanId(plan.getId())
                .billingCycle(request.getBillingCycle())
                .paymentMode(request.getPaymentMode())
                .status(SubscriptionStatus.TRIAL)
                .trialStartDate(now)
                .trialEndDate(now.plusDays(trialDays))
                .currentPeriodStart(now)
                .currentPeriodEnd(now.plusDays(trialDays))
                .gracePeriodDays(gracePeriodDays)
                .build();

        ShopSubscription savedSubscription = shopSubscriptionRepository.save(subscription);

        // Create default shop roles (Shop Manager + Shop User)
        UUID shopManagerRoleId = roleService.createDefaultShopRoles(tenantId, savedShop.getId());

        // Assign manager to shop
        User savedManager = null;
        if (hasManager) {
            if (existingManager) {
                // Existing user — just create assignment to new shop
                savedManager = userRepository.findByUsrEmail(request.getManagerEmail()).get();

                if (userShopAssignmentRepository.existsByUserIdAndShopIdAndIsActiveTrue(
                        savedManager.getUsrId(), savedShop.getId())) {
                    throw new IllegalArgumentException("Manager is already assigned to this shop");
                }

                UserShopAssignment managerAssignment = UserShopAssignment.builder()
                        .userId(savedManager.getUsrId())
                        .shopId(savedShop.getId())
                        .roleId(shopManagerRoleId)
                        .shopRole(UserType.SHOP_MANAGER)
                        .build();
                userShopAssignmentRepository.save(managerAssignment);
            } else {
                // New user — create user + assignment
                String randomPassword = generateRandomPassword();
                String encodedPassword = passwordEncoder.encode(randomPassword);
                String username = generateUsername(request.getManagerEmail());

                User manager = User.builder()
                        .tenantId(tenantId)
                        .username(username)
                        .usrFirstName(request.getManagerFirstName())
                        .usrLastName(request.getManagerLastName())
                        .usrEmail(request.getManagerEmail())
                        .usrPhoneNumber(request.getManagerPhone())
                        .usrPassword(encodedPassword)
                        .userType(UserType.SHOP_MANAGER)
                        .usrStatus(UserStatus.ACTIVE)
                        .isActive(true)
                        .emailVerified(true)
                        .emailVerifiedAt(now)
                        .mustChangePassword(true)
                        .passwordVersion(1)
                        .passwordChangedAt(now)
                        .passwordExpiresAt(now.plusDays(passwordExpiryDays))
                        .build();

                savedManager = userRepository.save(manager);

                UserShopAssignment managerAssignment = UserShopAssignment.builder()
                        .userId(savedManager.getUsrId())
                        .shopId(savedShop.getId())
                        .roleId(shopManagerRoleId)
                        .shopRole(UserType.SHOP_MANAGER)
                        .build();
                userShopAssignmentRepository.save(managerAssignment);

                sendManagerCredentialsEmail(savedManager, randomPassword, savedShop.getShopName());
            }
        }

        // Auto-assign default modules from business type (isDefault=true in business_type_module)
        if (request.getBusinessTypeId() != null) {
            List<BusinessTypeModule> defaultBtModules = businessTypeModuleRepository
                    .findByBusinessTypeIdAndIsDefaultTrue(request.getBusinessTypeId());
            List<UUID> defaultModuleIds = defaultBtModules.stream()
                    .map(BusinessTypeModule::getAdditionalModuleId)
                    .toList();
            saveAdditionalModules(savedShop.getId(), defaultModuleIds);
        }

        // Save additional (paid) modules selected by tenant
        saveAdditionalModules(savedShop.getId(), request.getAdditionalModuleIds());

        return buildShopResponse(savedShop, savedSubscription, plan, savedManager);
    }

    private ShopResponse updateShop(ShopRequest request, UUID tenantId) {
        Shop shop = shopRepository.findByIdAndTenantId(request.getId(), tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Shop not found"));

        // Update shop fields (ModelMapper skips nulls via setSkipNullEnabled)
        modelMapper.map(request, shop);
        shop.setTenantId(tenantId);

        Shop savedShop = shopRepository.save(shop);

        // Update subscription fields if provided
        ShopSubscription subscription = shopSubscriptionRepository.findByShopId(savedShop.getId()).orElse(null);
        if (subscription != null) {
            boolean subscriptionChanged = false;

            if (request.getSubscriptionPlanId() != null
                    && !request.getSubscriptionPlanId().equals(subscription.getSubscriptionPlanId())) {
                SubscriptionPlan newPlan = subscriptionPlanRepository.findById(request.getSubscriptionPlanId())
                        .orElseThrow(() -> new IllegalArgumentException("Subscription plan not found"));
                if (newPlan.getStatus() != SubscriptionPlanStatus.ACTIVE) {
                    throw new IllegalArgumentException("Subscription plan is not active");
                }
                subscription.setSubscriptionPlanId(newPlan.getId());
                subscriptionChanged = true;
            }
            if (request.getBillingCycle() != null) {
                subscription.setBillingCycle(request.getBillingCycle());
                subscriptionChanged = true;
            }
            if (request.getPaymentMode() != null) {
                subscription.setPaymentMode(request.getPaymentMode());
                subscriptionChanged = true;
            }
            if (subscriptionChanged) {
                subscription = shopSubscriptionRepository.save(subscription);
            }
        }

        // Sync additional modules if provided
        if (request.getAdditionalModuleIds() != null) {
            syncAdditionalModules(savedShop.getId(), request.getAdditionalModuleIds());
        }

        SubscriptionPlan plan = subscription != null
                ? subscriptionPlanRepository.findById(subscription.getSubscriptionPlanId()).orElse(null)
                : null;
        User manager = userShopAssignmentRepository
                .findByShopIdAndShopRoleAndIsActiveTrue(savedShop.getId(), UserType.SHOP_MANAGER)
                .stream().findFirst()
                .flatMap(a -> userRepository.findById(a.getUserId()))
                .orElse(null);

        return buildShopResponse(savedShop, subscription, plan, manager);
    }

    public ListResponse<ShopResponse> fetch(FetchRequest request) {
        UUID tenantId = SecurityContextUtil.getCurrentTenantId();

        if (request.getId() != null) {
            Shop shop = shopRepository.findByIdAndTenantId(request.getId(), tenantId)
                    .orElseThrow(() -> new IllegalArgumentException("Shop not found"));
            List<ShopResponse> result = List.of(buildFullShopResponse(shop));
            return ListResponse.of(result);
        }

        String search = request.getSearch();
        Integer limit = request.getLimit();

        if (limit == null) {
            List<Shop> all = shopRepository.searchAll(tenantId, search);
            List<ShopResponse> responses = all.stream()
                    .map(this::buildFullShopResponse)
                    .toList();
            return ListResponse.of(responses);
        }

        PageRequest pageRequest = PageRequest.of(request.getStart(), limit);
        Page<Shop> page = shopRepository.searchAll(tenantId, search, pageRequest);

        Page<ShopResponse> responsePage = page.map(this::buildFullShopResponse);
        return ListResponse.from(responsePage);
    }

    @Transactional
    public void delete(UUID id) {
        UUID tenantId = SecurityContextUtil.getCurrentTenantId();
        Shop shop = shopRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Shop not found"));
        shopRepository.delete(shop);
    }

    // ==================== ADDITIONAL MODULES ====================

    private void saveAdditionalModules(UUID shopId, List<UUID> moduleIds) {
        if (moduleIds == null || moduleIds.isEmpty()) return;

        for (UUID moduleId : moduleIds) {
            // Skip if already assigned to this shop
            if (shopAdditionalModuleRepository.existsByShopIdAndAdditionalModuleIdAndIsActiveTrue(shopId, moduleId)) {
                continue;
            }

            AdditionalModule module = additionalModuleRepository.findById(moduleId)
                    .orElseThrow(() -> new IllegalArgumentException("Module not found: " + moduleId));
            if (module.getStatus() != ModuleStatus.ACTIVE) {
                throw new IllegalArgumentException("Module is not active: " + module.getModuleName());
            }

            ShopAdditionalModule shopModule = ShopAdditionalModule.builder()
                    .shopId(shopId)
                    .additionalModuleId(moduleId)
                    .build();
            shopAdditionalModuleRepository.save(shopModule);
        }
    }

    private void syncAdditionalModules(UUID shopId, List<UUID> moduleIds) {
        List<ShopAdditionalModule> existing = shopAdditionalModuleRepository.findByShopIdAndIsActiveTrue(shopId);

        // Deactivate modules not in the new list
        for (ShopAdditionalModule sam : existing) {
            if (!moduleIds.contains(sam.getAdditionalModuleId())) {
                sam.setIsActive(false);
                shopAdditionalModuleRepository.save(sam);
            }
        }

        // Add new modules or reactivate existing ones
        List<UUID> existingActiveIds = existing.stream()
                .map(ShopAdditionalModule::getAdditionalModuleId)
                .toList();

        for (UUID moduleId : moduleIds) {
            if (!existingActiveIds.contains(moduleId)) {
                var opt = shopAdditionalModuleRepository.findByShopIdAndAdditionalModuleId(shopId, moduleId);
                if (opt.isPresent()) {
                    ShopAdditionalModule sam = opt.get();
                    sam.setIsActive(true);
                    sam.setSubscribedAt(LocalDateTime.now());
                    shopAdditionalModuleRepository.save(sam);
                } else {
                    AdditionalModule module = additionalModuleRepository.findById(moduleId)
                            .orElseThrow(() -> new IllegalArgumentException("Module not found: " + moduleId));
                    if (module.getStatus() != ModuleStatus.ACTIVE) {
                        throw new IllegalArgumentException("Module is not active: " + module.getModuleName());
                    }
                    ShopAdditionalModule shopModule = ShopAdditionalModule.builder()
                            .shopId(shopId)
                            .additionalModuleId(moduleId)
                            .build();
                    shopAdditionalModuleRepository.save(shopModule);
                }
            }
        }
    }

    private List<ShopResponse.ModuleInfo> resolveModuleInfos(List<BusinessTypeModule> btModules) {
        return btModules.stream()
                .map(btm -> {
                    AdditionalModule module = additionalModuleRepository.findById(btm.getAdditionalModuleId())
                            .orElse(null);
                    if (module == null) return null;
                    return ShopResponse.ModuleInfo.builder()
                            .id(module.getId())
                            .moduleCode(module.getModuleCode())
                            .moduleName(module.getModuleName())
                            .build();
                })
                .filter(java.util.Objects::nonNull)
                .toList();
    }

    private List<ShopResponse.ModuleInfo> buildModuleInfos(List<ShopAdditionalModule> shopModules) {
        return shopModules.stream()
                .map(sam -> {
                    AdditionalModule module = additionalModuleRepository.findById(sam.getAdditionalModuleId())
                            .orElse(null);
                    if (module == null) return null;
                    return ShopResponse.ModuleInfo.builder()
                            .id(module.getId())
                            .moduleCode(module.getModuleCode())
                            .moduleName(module.getModuleName())
                            .subscribedAt(sam.getSubscribedAt())
                            .build();
                })
                .filter(java.util.Objects::nonNull)
                .toList();
    }

    // ==================== HELPER METHODS ====================

    private ShopResponse buildFullShopResponse(Shop shop) {
        ShopSubscription subscription = shopSubscriptionRepository.findByShopId(shop.getId()).orElse(null);
        SubscriptionPlan plan = subscription != null
                ? subscriptionPlanRepository.findById(subscription.getSubscriptionPlanId()).orElse(null)
                : null;
        User manager = userShopAssignmentRepository
                .findByShopIdAndShopRoleAndIsActiveTrue(shop.getId(), UserType.SHOP_MANAGER)
                .stream().findFirst()
                .flatMap(a -> userRepository.findById(a.getUserId()))
                .orElse(null);
        return buildShopResponse(shop, subscription, plan, manager);
    }

    private ShopResponse buildShopResponse(Shop shop, ShopSubscription subscription, SubscriptionPlan plan, User manager) {
        ShopResponse response = modelMapper.map(shop, ShopResponse.class);

        // Resolve business type name
        if (shop.getBusinessTypeId() != null) {
            String businessTypeName = businessTypeRepository.findByIdAndIsActiveTrue(shop.getBusinessTypeId())
                    .map(BusinessType::getName)
                    .orElse(null);
            response.setBusinessTypeName(businessTypeName);
        }

        // Subscription info
        if (subscription != null) {
            response.setSubscriptionPlanId(subscription.getSubscriptionPlanId());
            response.setBillingCycle(subscription.getBillingCycle());
            response.setPaymentMode(subscription.getPaymentMode());
            response.setSubscriptionStatus(subscription.getStatus());
            response.setTrialEndDate(subscription.getTrialEndDate());
            response.setCurrentPeriodEnd(subscription.getCurrentPeriodEnd());
            response.setGraceUntil(subscription.getGraceUntil());
        }

        if (plan != null) {
            response.setSubscriptionPlanName(plan.getPlanName());
        }

        // Manager info
        if (manager != null) {
            response.setManagerId(manager.getUsrId());
            response.setManagerName(manager.getFullName());
            response.setManagerEmail(manager.getUsrEmail());
        }

        // Default modules — from business type definition
        if (shop.getBusinessTypeId() != null) {
            List<BusinessTypeModule> defaultBtModules = businessTypeModuleRepository
                    .findByBusinessTypeIdAndIsDefaultTrue(shop.getBusinessTypeId());
            response.setDefaultModules(resolveModuleInfos(defaultBtModules));
        }

        // Additional modules — what the shop actually subscribed to (paid extras)
        response.setAdditionalModules(buildModuleInfos(
                shopAdditionalModuleRepository.findByShopIdAndIsActiveTrue(shop.getId())));

        return response;
    }

    private String generateShopCode() {
        long count = shopRepository.count();
        String code;
        do {
            count++;
            code = String.format("SHP-%04d", count);
        } while (shopRepository.existsByShopCode(code));
        return code;
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

    private void sendManagerCredentialsEmail(User manager, String password, String shopName) {
        String subject = "Your Shop Manager Account - " + shopName;
        String body = String.format("""
                Hello %s,

                You have been assigned as the Shop Manager for %s.

                Here are your login credentials:
                Email: %s
                Temporary Password: %s

                Please login and change your password immediately.

                Regards,
                POS System
                """,
                manager.getUsrFirstName(),
                shopName,
                manager.getUsrEmail(),
                password
        );

        try {
            emailService.sendSimpleEmail(manager.getUsrEmail(), subject, body);
        } catch (Exception e) {
            log.error("Failed to send credentials email to manager: {}", manager.getUsrEmail(), e);
        }
    }

}
