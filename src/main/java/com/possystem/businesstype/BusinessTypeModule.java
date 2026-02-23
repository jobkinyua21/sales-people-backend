package com.possystem.businesstype;

import com.possystem.audit.Auditable;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "business_type_module", schema = "pos_core",
        uniqueConstraints = @UniqueConstraint(columnNames = {"business_type_id", "additional_module_id"}))
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
public class BusinessTypeModule extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @EqualsAndHashCode.Include
    private UUID id;

    @Column(name = "business_type_id", nullable = false)
    private UUID businessTypeId;

    @Column(name = "additional_module_id", nullable = false)
    private UUID additionalModuleId;

    @Column(name = "is_default", nullable = false)
    @Builder.Default
    private Boolean isDefault = false;
}
