package com.possystem.subscription;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SubscriptionPlanResponse {

    private UUID id;
    private String planCode;
    private String planName;
    private String planType;
    private String billingLevel;
    private BigDecimal priceMonthly;
    private BigDecimal priceYearly;
    private String currency;
    private Integer maxUsers;
    private String modulesIncluded;
    private SubscriptionPlanStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
