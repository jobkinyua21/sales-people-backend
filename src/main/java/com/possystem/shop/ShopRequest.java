package com.possystem.shop;

import com.possystem.shop.enums.BillingCycle;
import com.possystem.shop.enums.PaymentMode;
import com.possystem.shop.enums.ShopStatus;
import jakarta.validation.constraints.NotBlank;
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

    @NotBlank(message = "Shop name is required")
    private String shopName;

    private String address;

    private String city;

    private String country;

    private String phone;

    private String email;

    private String logoUrl;

    private ShopStatus status;

    // Subscription fields (required on create, validated in service)
    private UUID subscriptionPlanId;

    private BillingCycle billingCycle;

    private PaymentMode paymentMode;

    // Shop manager fields (required on create, validated in service)
    private String managerFirstName;

    private String managerLastName;

    private String managerEmail;

    private String managerPhone;

    // Additional modules to subscribe to
    private List<UUID> additionalModuleIds;
}
