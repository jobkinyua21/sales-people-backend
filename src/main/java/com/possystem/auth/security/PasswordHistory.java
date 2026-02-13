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
@Table(name = "password_history", schema = "pos_core")
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class PasswordHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id")
    @EqualsAndHashCode.Include
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID usrId;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
