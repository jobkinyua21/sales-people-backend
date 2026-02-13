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
@Table(name = "user_session", schema = "pos_core")
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class UserSession {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id")
    @EqualsAndHashCode.Include
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID usrId;

    @Column(name = "session_token", unique = true, nullable = false, columnDefinition = "TEXT")
    private String sessionToken;

    @Column(name = "refresh_token", unique = true, nullable = false, columnDefinition = "TEXT")
    private String refreshToken;

    @Column(name = "current_tenant_id")
    private UUID currentTenantId;

    @Column(name = "current_shop_id")
    private UUID currentShopId;

    @Column(name = "current_context", length = 20)
    @Builder.Default
    private String currentContext = "TENANT";

    @Column(name = "ip_address")
    private String ipAddress;

    @Column(name = "user_agent", columnDefinition = "TEXT")
    private String userAgent;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "last_activity_at")
    private LocalDateTime lastActivityAt;
}
