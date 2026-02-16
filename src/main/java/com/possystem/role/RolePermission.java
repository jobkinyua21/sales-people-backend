package com.possystem.role;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "role_permission", schema = "pos_core",
        uniqueConstraints = @UniqueConstraint(columnNames = {"role_id", "permission_id"}))
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class RolePermission {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @EqualsAndHashCode.Include
    private UUID id;

    @Column(name = "role_id", nullable = false)
    private UUID roleId;

    @Column(name = "permission_id", nullable = false)
    private UUID permissionId;
}
