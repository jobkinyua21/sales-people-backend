package com.possystem.auth.security;

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
@Table(name = "two_factor_auth", schema = "pos_core")
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class TwoFactorAuth {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id")
    @EqualsAndHashCode.Include
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID usrId;

    @Column(name = "is_enabled", nullable = false)
    @Builder.Default
    private Boolean isEnabled = false;

    @Column(name = "secret_key")
    private String secretKey;

    @Column(name = "backup_codes", columnDefinition = "TEXT")
    private String backupCodes;

    @Column(name = "preferred_method", length = 10)
    @Builder.Default
    private String preferredMethod = "TOTP";

    @Column(name = "phone_number", length = 20)
    private String phoneNumber;

    @Column(name = "phone_verified", nullable = false)
    @Builder.Default
    private Boolean phoneVerified = false;

    @Column(name = "enabled_at")
    private LocalDateTime enabledAt;

    @Column(name = "last_used_at")
    private LocalDateTime lastUsedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
