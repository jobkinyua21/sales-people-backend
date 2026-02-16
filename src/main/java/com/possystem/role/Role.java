package com.possystem.role;

import com.possystem.audit.Auditable;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "role", schema = "pos_core")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
public class Role extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @EqualsAndHashCode.Include
    private UUID id;

    @Column(name = "role_code", nullable = false, unique = true, length = 50)
    private String roleCode;

    @Column(name = "role_name", nullable = false, length = 100)
    private String roleName;

    @Enumerated(EnumType.STRING)
    @Column(name = "role_type", nullable = false, length = 20)
    @Builder.Default
    private RoleType roleType = RoleType.CUSTOM;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "description", length = 255)
    private String description;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;
}
