package com.possystem.menu;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "menu", schema = "pos_core")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Menu {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @EqualsAndHashCode.Include
    private UUID id;

    @Column(name = "menu_code", nullable = false, unique = true, length = 50)
    private String menuCode;

    @Column(name = "menu_name", nullable = false, length = 100)
    private String menuName;

    @Column(name = "menu_link", length = 100)
    private String menuLink;

    @Column(name = "menu_icon", length = 50)
    private String menuIcon;

    @Column(name = "parent_id")
    private UUID parentId;

    @Column(name = "sort_order")
    @Builder.Default
    private Integer sortOrder = 0;

    @Column(name = "module", length = 50)
    private String module;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;
}
