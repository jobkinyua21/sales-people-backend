package com.possystem.shop;

import com.possystem.audit.Auditable;
import com.possystem.shop.enums.BillingCycle;
import com.possystem.shop.enums.PaymentMode;
import com.possystem.shop.enums.SubscriptionStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "shop_subscription", schema = "pos_core")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
public class ShopSubscription extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @EqualsAndHashCode.Include
    private UUID id;

    @Column(name = "shop_id", nullable = false)
    private UUID shopId;

    @Column(name = "subscription_plan_id", nullable = false)
    private UUID subscriptionPlanId;

    @Enumerated(EnumType.STRING)
    @Column(name = "billing_cycle", nullable = false, length = 20)
    private BillingCycle billingCycle;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_mode", nullable = false, length = 20)
    private PaymentMode paymentMode;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private SubscriptionStatus status = SubscriptionStatus.TRIAL;

    @Column(name = "trial_start_date")
    private LocalDateTime trialStartDate;

    @Column(name = "trial_end_date")
    private LocalDateTime trialEndDate;

    @Column(name = "current_period_start")
    private LocalDateTime currentPeriodStart;

    @Column(name = "current_period_end")
    private LocalDateTime currentPeriodEnd;

    @Column(name = "grace_period_days")
    @Builder.Default
    private Integer gracePeriodDays = 7;

    @Column(name = "grace_until")
    private LocalDateTime graceUntil;

    @Column(name = "suspended_at")
    private LocalDateTime suspendedAt;

    @Column(name = "cancellation_scheduled_for")
    private LocalDateTime cancellationScheduledFor;

    @Column(name = "permanently_cancelled_at")
    private LocalDateTime permanentlyCancelledAt;
}
