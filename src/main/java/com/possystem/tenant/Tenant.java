package com.possystem.tenant;

import com.possystem.audit.Auditable;
import com.possystem.common.UserStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
@Entity
@Table(name = "tenant", schema = "pos_core")
public class Tenant extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "tenant_id")
    @EqualsAndHashCode.Include
    private UUID tenantId;

    @Column(name = "tenant_code", unique = true, nullable = false, length = 20)
    private String tenantCode;

    @Column(name = "user_id", nullable = false)
    private UUID usrId;

    @Column(name = "business_name", nullable = false, length = 100)
    private String businessName;

    @Column(name = "business_registration_number", length = 50)
    private String businessRegistrationNumber;

    @Column(name = "business_type", length = 50)
    private String businessType;

    @Column(name = "business_address", length = 255)
    private String businessAddress;

    @Column(name = "business_email", length = 100)
    private String businessEmail;

    @Column(name = "business_phone", length = 20)
    private String businessPhone;

    @Column(name = "email", unique = true, length = 100)
    private String email;

    @Column(name = "phone", length = 20)
    private String phone;

    @Column(name = "country", length = 50)
    private String country;

    @Column(name = "city", length = 50)
    private String city;

    @Column(name = "logo_url", length = 255)
    private String logoUrl;

    @Column(name = "subscription_plan", length = 50)
    private String subscriptionPlan;

    @Column(name = "subscription_status", length = 30)
    private String subscriptionStatus;

    @Column(name = "subscription_expires_at")
    private LocalDateTime subscriptionExpiresAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    @Builder.Default
    private UserStatus status = UserStatus.PENDING;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "is_verified", nullable = false)
    @Builder.Default
    private Boolean isVerified = false;
}
