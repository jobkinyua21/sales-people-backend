package com.possystem.subscription;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
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

    private Integer maxUsers;

    private String modulesIncluded;

    private String status;
}
