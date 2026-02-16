package com.possystem.subscription;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionPlanRequest {

    private UUID id;

    @NotBlank(message = "Plan name is required")
    private String planName;

    @NotBlank(message = "Plan type is required")
    private String planType;

    @NotBlank(message = "Billing level is required")
    private String billingLevel;

    @NotNull(message = "Monthly price is required")
    private BigDecimal priceMonthly;

    @NotNull(message = "Yearly price is required")
    private BigDecimal priceYearly;

    private String description;

    private Integer maxUsers;

    private Integer maxProducts;

    private Integer maxTerminals;

    private Integer maxCustomers;

    private Integer maxShops;

    private String reportLevel;

    private List<String> modulesIncluded;

    private SubscriptionPlanStatus status;
}
