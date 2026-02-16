package com.possystem.shop;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.possystem.shop.enums.BillingCycle;
import com.possystem.shop.enums.PaymentMode;
import com.possystem.shop.enums.ShopStatus;
import com.possystem.shop.enums.SubscriptionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ShopResponse {

    private UUID id;
    private String shopCode;
    private String shopName;
    private String address;
    private String city;
    private String country;
    private String phone;
    private String email;
    private String logoUrl;
    private ShopStatus status;

    // Subscription info
    private UUID subscriptionPlanId;
    private String subscriptionPlanName;
    private BillingCycle billingCycle;
    private PaymentMode paymentMode;
    private SubscriptionStatus subscriptionStatus;
    private LocalDateTime trialEndDate;
    private LocalDateTime currentPeriodEnd;
    private LocalDateTime graceUntil;

    // Manager info
    private UUID managerId;
    private String managerName;
    private String managerEmail;

    // Additional modules
    private List<AdditionalModuleInfo> additionalModules;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AdditionalModuleInfo {
        private UUID id;
        private String moduleCode;
        private String moduleName;
        private LocalDateTime subscribedAt;
    }
}
