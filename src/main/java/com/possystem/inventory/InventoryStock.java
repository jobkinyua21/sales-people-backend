package com.possystem.inventory;

import com.possystem.audit.Auditable;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "inventory_stock", schema = "pos_core", indexes = {
        @Index(name = "idx_stock_shop_active", columnList = "shop_id, is_active"),
        @Index(name = "idx_stock_variant", columnList = "variant_id, shop_id", unique = true)
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
public class InventoryStock extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @EqualsAndHashCode.Include
    private UUID id;

    @Column(name = "shop_id", nullable = false)
    private UUID shopId;

    @Column(name = "variant_id", nullable = false)
    private UUID variantId;

    @Column(name = "current_quantity", nullable = false, precision = 12, scale = 3)
    @Builder.Default
    private BigDecimal currentQuantity = BigDecimal.ZERO;

    @Column(name = "reorder_level", precision = 12, scale = 3)
    private BigDecimal reorderLevel;

    @Column(name = "reorder_quantity", precision = 12, scale = 3)
    private BigDecimal reorderQuantity;

    @Column(name = "last_restocked_at")
    private LocalDateTime lastRestockedAt;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;
}
