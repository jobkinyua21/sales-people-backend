package com.possystem.generalsetting.users;

import com.possystem.audit.Auditable;
import com.possystem.auth.pos.PosProfile;
import com.possystem.common.UserStatus;
import com.fasterxml.jackson.annotation.JsonIgnore;
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
@Table(name = "shop_user", schema = "pos_core")
public class ShopUser extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "user_id")
    @EqualsAndHashCode.Include
    private UUID userId;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "shop_id", nullable = false)
    private UUID shopId;

    @Column(name = "email")
    private String email;

    @Column(name = "phone_number", nullable = false)
    private String phoneNumber;

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "last_name", nullable = false)
    private String lastName;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    @Builder.Default
    private UserStatus status = UserStatus.PENDING;

    @JsonIgnore
    @Column(name = "password")
    private String password;

    @JsonIgnore
    @Column(name = "pin")
    private String pin;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "pos_profile_id", referencedColumnName = "profile_id")
    private PosProfile posProfile;

    @Column(name = "last_login")
    private LocalDateTime lastLogin;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Transient
    public String getFullName() {
        return (firstName != null ? firstName + " " : "") +
               (lastName != null ? lastName : "");
    }
}
