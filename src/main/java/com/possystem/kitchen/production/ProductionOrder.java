package com.possystem.kitchen.production;

import com.possystem.audit.Auditable;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "production_order", schema = "pos_core", indexes = {
        @Index(name = "idx_prod_order_shop", columnList = "shop_id"),
        @Index(name = "idx_prod_order_recipe", columnList = "recipe_id"),
        @Index(name = "idx_prod_order_status", columnList = "status"),
        @Index(name = "idx_prod_order_created_at", columnList = "created_at DESC")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
public class ProductionOrder extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @EqualsAndHashCode.Include
    private UUID id;

    @Column(name = "shop_id", nullable = false)
    private UUID shopId;

    @Column(name = "production_number", nullable = false, length = 20)
    private String productionNumber;

    @Column(name = "recipe_id", nullable = false)
    private UUID recipeId;

    // Snapshot fields
    @Column(name = "recipe_name", length = 200)
    private String recipeName;

    @Column(name = "product_name", length = 200)
    private String productName;

    @Column(name = "variant_name", length = 200)
    private String variantName;

    // The finished product variant that will be stocked
    @Column(name = "variant_id", nullable = false)
    private UUID variantId;

    // How many units to produce (e.g., 200 chapatis)
    @Column(name = "quantity_to_produce", nullable = false, precision = 12, scale = 3)
    private BigDecimal quantityToProduce;

    // Actual quantity produced (may differ due to waste/bonus)
    @Column(name = "quantity_produced", precision = 12, scale = 3)
    private BigDecimal quantityProduced;

    // Yield unit from recipe
    @Column(name = "yield_unit", length = 50)
    private String yieldUnit;

    // Estimated ingredient cost for this production run
    @Column(name = "estimated_cost", precision = 12, scale = 2)
    private BigDecimal estimatedCost;

    // Actual cost (same as estimated unless recipe changes mid-run)
    @Column(name = "actual_cost", precision = 12, scale = 2)
    private BigDecimal actualCost;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private ProductionStatus status = ProductionStatus.PENDING;

    @Column(name = "notes", length = 1000)
    private String notes;

    // Who created the production order
    @Column(name = "requested_by", nullable = false)
    private UUID requestedBy;

    // Who actually produced it (chef)
    @Column(name = "produced_by")
    private UUID producedBy;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @Column(name = "cancel_reason", length = 500)
    private String cancelReason;

    // Whether ingredients have been deducted
    @Column(name = "ingredients_deducted", nullable = false)
    @Builder.Default
    private Boolean ingredientsDeducted = false;

    // Whether finished goods have been added to stock
    @Column(name = "stock_added", nullable = false)
    @Builder.Default
    private Boolean stockAdded = false;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;
}
