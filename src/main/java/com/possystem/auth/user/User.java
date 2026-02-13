package com.possystem.auth.user;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.possystem.audit.Auditable;
import com.possystem.common.UserStatus;
import com.possystem.common.UserType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "user", schema = "pos_core")
@ToString(onlyExplicitlyIncluded = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
public class User extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "usr_id")
    @EqualsAndHashCode.Include
    private UUID usrId;

    @Column(name = "tenant_id")
    private UUID tenantId;

    @Column(name = "username", unique = true)
    private String username;

    @Column(name = "email", unique = true)
    private String usrEmail;

    @Column(name = "phone_number", unique = true, nullable = false)
    private String usrPhoneNumber;

    @Column(name = "first_name", nullable = false)
    private String usrFirstName;

    @Column(name = "last_name", nullable = false)
    private String usrLastName;

    @JsonIgnore
    @Column(name = "password")
    private String usrPassword;

    @JsonIgnore
    @Column(name = "password_salt")
    private String passwordSalt;

    @Column(name = "password_version")
    @Builder.Default
    private Integer passwordVersion = 1;

    @Column(name = "password_changed_at")
    private LocalDateTime passwordChangedAt;

    @Column(name = "password_expires_at")
    private LocalDateTime passwordExpiresAt;

    @Column(name = "must_change_password", nullable = false)
    @Builder.Default
    private Boolean mustChangePassword = false;

    @Column(name = "failed_login_attempts", nullable = false, columnDefinition = "integer default 0")
    @Builder.Default
    private Integer failedLoginAttempts = 0;

    @Column(name = "locked_until")
    private LocalDateTime lockedUntil;

    @JsonIgnore
    @Column(name = "pin")
    private String usrPin;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    @Builder.Default
    private UserStatus usrStatus = UserStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(name = "user_type", nullable = false)
    @Builder.Default
    private UserType userType = UserType.TENANT_ADMIN;

    @Column(name = "email_verified", nullable = false)
    @Builder.Default
    private Boolean emailVerified = false;

    @Column(name = "email_verification_token")
    private String emailVerificationToken;

    @Column(name = "email_verified_at")
    private LocalDateTime emailVerifiedAt;

    @Column(name = "last_login")
    private LocalDateTime usrLastLogin;

    @Column(name = "last_login_ip")
    private String lastLoginIp;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Transient
    public String getFullName() {
        return (usrFirstName != null ? usrFirstName + " " : "") +
               (usrLastName != null ? usrLastName : "");
    }
}
