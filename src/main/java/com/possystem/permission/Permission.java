package com.possystem.permission;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "permission", schema = "pos_core")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Permission {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @EqualsAndHashCode.Include
    private UUID id;

    @Column(name = "permission_code", nullable = false, unique = true, length = 50)
    private String permissionCode;

    @Column(name = "permission_name", nullable = false, length = 100)
    private String permissionName;

    @Column(name = "module", nullable = false, length = 50)
    private String module;

    @Enumerated(EnumType.STRING)
    @Column(name = "action", nullable = false, length = 20)
    private PermissionAction action;

    @Column(name = "description", length = 255)
    private String description;
}
