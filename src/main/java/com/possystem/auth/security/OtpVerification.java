package com.possystem.auth.security;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "otp_verification", schema = "pos_core")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OtpVerification {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "usr_id", nullable = false)
    private UUID usrId;

    @Column(name = "user_type", nullable = false)
    private String userType;

    @Column(name = "otp_code", nullable = false, length = 4)
    private String otpCode;

    @Column(name = "email", nullable = false)
    private String email;

    @Column(name = "is_used", nullable = false)
    @Builder.Default
    private Boolean isUsed = false;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "used_at")
    private LocalDateTime usedAt;

    @Column(name = "ip_address")
    private String ipAddress;

    @Column(name = "attempt_count", nullable = false, columnDefinition = "integer default 0")
    @Builder.Default
    private Integer attemptCount = 0;

    @Column(name = "selected_shop_id")
    private UUID selectedShopId;
}
