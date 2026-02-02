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
@Table(
        name = "package_module",
        schema = "pos_core",
        uniqueConstraints = @UniqueConstraint(columnNames = {"package_id", "module_id"})
)
public class PackageModule extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "package_module_id")
    @EqualsAndHashCode.Include
    private UUID packageModuleId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "package_id", nullable = false)
    private PosPackage posPackage;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "module_id", nullable = false)
    private PosModule posModule;

    @Column(name = "is_included", nullable = false)
    @Builder.Default
    private Boolean isIncluded = false;

    @Column(name = "additional_cost", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal additionalCost = BigDecimal.ZERO;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;
}
