package com.possystem.module;

import com.possystem.audit.Auditable;
import com.possystem.module.enums.ModuleStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "additional_module", schema = "pos_core")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
public class AdditionalModule extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @EqualsAndHashCode.Include
    private UUID id;

    @Column(name = "module_code", nullable = false, unique = true, length = 50)
    private String moduleCode;

    @Column(name = "module_name", nullable = false, length = 100)
    private String moduleName;

    @Column(name = "description", length = 255)
    private String description;

    @Column(name = "monthly_price", nullable = false, precision = 15, scale = 2)
    private BigDecimal monthlyPrice;

    @Column(name = "yearly_price", precision = 15, scale = 2)
    private BigDecimal yearlyPrice;

    @Column(name = "currency", nullable = false, length = 3)
    @Builder.Default
    private String currency = "KES";

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private ModuleStatus status = ModuleStatus.ACTIVE;
}
