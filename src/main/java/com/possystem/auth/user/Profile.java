package com.possystem.auth.user;

import com.possystem.audit.Auditable;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

/**
 * Profile entity representing user profiles in the system.
 * A profile is linked to permissions through the profile_permission table.
 * Users are assigned to one profile, and profiles can have multiple permissions with granular access control.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
@Entity
@Table(name = "profile", schema = "pos_core")
public class Profile extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "profile_id")
    private UUID profileId;

    @Column(name = "profile_name", unique = true, nullable = false, length = 100)
    private String profileName;

    @Column(name = "profile_code", unique = true, nullable = false, length = 100)
    private String profileCode;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;
}