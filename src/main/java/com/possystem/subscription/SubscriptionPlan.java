package com.possystem.subscription;

import com.possystem.audit.Auditable;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "subscription_plan", schema = "pos_core")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
public class SubscriptionPlan extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @EqualsAndHashCode.Include
    private UUID id;

    @Column(name = "plan_code", nullable = false, unique = true, length = 50)
    private String planCode;

    @Column(name = "plan_name", nullable = false, length = 100)
    private String planName;

    @Column(name = "plan_type", nullable = false, length = 20)
    private String planType;

    @Column(name = "billing_level", nullable = false, length = 20)
    private String billingLevel;

    @Column(name = "price_monthly", nullable = false, precision = 15, scale = 2)
    private BigDecimal priceMonthly;

    @Column(name = "price_yearly", nullable = false, precision = 15, scale = 2)
    private BigDecimal priceYearly;

    @Column(name = "currency", nullable = false, length = 3)
    @Builder.Default
    private String currency = "KES";

    @Column(name = "max_users")
    private Integer maxUsers;

    @Column(name = "modules_included", columnDefinition = "TEXT")
    private String modulesIncluded;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20, columnDefinition = "varchar(20) default 'ACTIVE'")
    @Builder.Default
    private SubscriptionPlanStatus status = SubscriptionPlanStatus.ACTIVE;
}
