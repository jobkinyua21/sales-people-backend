package com.possystem.generalsetting.packages;

import com.possystem.audit.Auditable;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
@Entity
@Table(name = "pos_module", schema = "pos_core")
public class PosModule extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "module_id")
    @EqualsAndHashCode.Include
    private UUID moduleId;

    @Column(name = "module_name", nullable = false, length = 100)
    private String moduleName;

    @Column(name = "module_code", unique = true, nullable = false, length = 50)
    private String moduleCode;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "base_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal basePrice;

    @Column(name = "is_core", nullable = false)
    @Builder.Default
    private Boolean isCore = false;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;
}
