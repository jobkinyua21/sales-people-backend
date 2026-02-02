package com.possystem.auth.user;

import com.possystem.audit.Auditable;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

/**
 * ProfilePermission entity representing the relationship between Profile and Permission
 * with granular permission flags (CRUD + Approve + Export).
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
        name = "profile_permission",
        schema = "pos_core",
        uniqueConstraints = @UniqueConstraint(columnNames = {"profile_id", "permission_id"})
)
public class ProfilePermission extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "profile_permission_id", nullable = false)
    private UUID profilePermissionId;

    // ------------------------
    // RELATIONSHIPS
    // ------------------------

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "profile_id", nullable = false)
    private Profile profile;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "permission_id", nullable = false)
    private Permission permission;

    // ------------------------
    // PERMISSION FLAGS
    // ------------------------

    @Column(name = "can_read", nullable = false)
    @Builder.Default
    private Boolean canRead = false;

    @Column(name = "can_write", nullable = false)
    @Builder.Default
    private Boolean canWrite = false;

    @Column(name = "can_create", nullable = false)
    @Builder.Default
    private Boolean canCreate = false;

    @Column(name = "can_approve", nullable = false)
    @Builder.Default
    private Boolean canApprove = false;

    @Column(name = "can_export", nullable = false)
    @Builder.Default
    private Boolean canExport = false;
}
