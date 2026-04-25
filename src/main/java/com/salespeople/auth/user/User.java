package com.salespeople.auth.user;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.salespeople.audit.Auditable;
import com.salespeople.common.UserStatus;
import com.salespeople.common.UserType;
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
@Table(name = "user", schema = "sales_people", indexes = {
        @Index(name = "idx_user_role", columnList = "role_id"),
        @Index(name = "idx_user_status", columnList = "status"),
        @Index(name = "idx_user_email", columnList = "email"),
        @Index(name = "idx_user_phone", columnList = "phone_number"),
        @Index(name = "idx_user_active", columnList = "is_active")
})
@ToString(onlyExplicitlyIncluded = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
public class User extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "usr_id")
    @EqualsAndHashCode.Include
    private UUID usrId;

    @Column(name = "role_id")
    private UUID roleId;

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

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    @Builder.Default
    private UserStatus usrStatus = UserStatus.ACTIVE;

    @Enumerated(EnumType.STRING)
    @Column(name = "user_type", nullable = false)
    @Builder.Default
    private UserType userType = UserType.SALES_PERSON;

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

    @Column(name = "staff_number", unique = true)
    private Integer staffNumber;

    @Transient
    public String getFullName() {
        return (usrFirstName != null ? usrFirstName + " " : "") +
               (usrLastName != null ? usrLastName : "");
    }
}
