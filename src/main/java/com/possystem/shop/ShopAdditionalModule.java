package com.possystem.shop;

import com.possystem.audit.Auditable;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "shop_additional_module", schema = "pos_core",
        uniqueConstraints = @UniqueConstraint(columnNames = {"shop_id", "additional_module_id"}))
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
public class ShopAdditionalModule extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @EqualsAndHashCode.Include
    private UUID id;

    @Column(name = "shop_id", nullable = false)
    private UUID shopId;

    @Column(name = "additional_module_id", nullable = false)
    private UUID additionalModuleId;

    @Column(name = "subscribed_at", nullable = false)
    @Builder.Default
    private LocalDateTime subscribedAt = LocalDateTime.now();

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;
}
