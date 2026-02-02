package com.possystem.auth.pos;

import com.possystem.audit.Auditable;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
        name = "pos_permission",
        schema = "pos_core",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"permission_name"}),
                @UniqueConstraint(columnNames = {"permission_code"})
        }
)
public class PosPermission extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "permission_id", nullable = false)
    private UUID permissionId;

    @Column(name = "permission_name", nullable = false, length = 100)
    private String permissionName;

    @Column(name = "permission_code", nullable = false, length = 100)
    private String permissionCode;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;
}
