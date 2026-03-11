package com.possystem.kitchen.recipe;

import com.possystem.audit.Auditable;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "recipe_ingredient", schema = "pos_core", indexes = {
        @Index(name = "idx_recipe_ingredient_recipe", columnList = "recipe_id"),
        @Index(name = "idx_recipe_ingredient_variant", columnList = "ingredient_variant_id")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
public class RecipeIngredient extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @EqualsAndHashCode.Include
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipe_id", nullable = false)
    private Recipe recipe;

    // The ingredient variant (e.g., "Flour 1kg", "Sugar 500g")
    @Column(name = "ingredient_variant_id", nullable = false)
    private UUID ingredientVariantId;

    // Snapshot fields for display
    @Column(name = "ingredient_name", nullable = false, length = 200)
    private String ingredientName;

    @Column(name = "variant_name", length = 200)
    private String variantName;

    @Column(name = "sku", length = 100)
    private String sku;

    // Quantity of this ingredient needed for the FULL recipe yield
    // e.g., 1kg flour for the recipe that yields 20 chapatis
    @Column(name = "quantity_required", nullable = false, precision = 12, scale = 3)
    private BigDecimal quantityRequired;

    // Unit of measure (should match inventory UOM for this variant)
    @Column(name = "unit_of_measure", length = 50)
    private String unitOfMeasure;

    // Cost of this ingredient at recipe creation time
    @Column(name = "unit_cost", precision = 12, scale = 2)
    private BigDecimal unitCost;

    // Waste percentage (e.g., 5% waste when peeling potatoes)
    @Column(name = "waste_percentage", precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal wastePercentage = BigDecimal.ZERO;

    // Whether this ingredient is optional (e.g., garnish)
    @Column(name = "is_optional", nullable = false)
    @Builder.Default
    private Boolean isOptional = false;

    @Column(name = "notes", length = 500)
    private String notes;
}
