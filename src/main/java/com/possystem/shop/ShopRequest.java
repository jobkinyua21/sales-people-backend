package com.possystem.shop;

import com.possystem.common.ValidationGroups;
import com.possystem.shop.enums.BillingCycle;
import com.possystem.shop.enums.PaymentMode;
import com.possystem.shop.enums.ShopStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShopRequest {

    private UUID id;

    private UUID businessTypeId;

    @NotBlank(message = "Shop name is required", groups = ValidationGroups.Create.class)
    private String shopName;

    private String address;

    private String city;

    private String country;

    private String phone;

    private String email;

    private String logoUrl;

    private ShopStatus status;

    @NotNull(message = "Subscription plan is required", groups = ValidationGroups.Create.class)
    private UUID subscriptionPlanId;

    @NotNull(message = "Billing cycle is required", groups = ValidationGroups.Create.class)
    private BillingCycle billingCycle;

    @NotNull(message = "Payment mode is required", groups = ValidationGroups.Create.class)
    private PaymentMode paymentMode;

    // Shop manager fields (validated in service on create)
    private String managerFirstName;

    private String managerLastName;

    private String managerEmail;

    private String managerPhone;

    // Additional modules to subscribe to
    private List<UUID> additionalModuleIds;
}
