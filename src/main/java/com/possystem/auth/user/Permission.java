package com.possystem.auth.user;

import com.possystem.audit.Auditable;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

/**
 * Permission entity representing individual granular permissions in the system.
 * Permissions are the atomic units of authorization and can be combined
 * with CRUD flags through ProfilePermission.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
        name = "permission",
        schema = "pos_core",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"permission_name"}),
                @UniqueConstraint(columnNames = {"permission_code"})
        }
)
public class Permission extends Auditable {

    // ------------------------
    // PRIMARY KEY
    // ------------------------

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "permission_id", nullable = false)
    private UUID permissionId;

    // ------------------------
    // BASIC FIELDS
    // ------------------------

    /**
     * Human-readable name of the permission/module.
     * Example: "Customer", "Loans", "User Management".
     */
    @Column(name = "permission_name", nullable = false, length = 100)
    private String permissionName;

    /**
     * Machine-friendly code used for enum-like mappings.
     * Example: "CUSTOMER", "LOAN", "USER_MGMT".
     */
    @Column(name = "permission_code", nullable = false, length = 100)
    private String permissionCode;

    @Column(name = "description", length = 500)
    private String description;

    // ------------------------
    // STATUS
    // ------------------------

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;
}
