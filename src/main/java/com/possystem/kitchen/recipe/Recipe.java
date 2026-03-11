package com.possystem.kitchen.recipe;

import com.possystem.audit.Auditable;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "recipe", schema = "pos_core", indexes = {
        @Index(name = "idx_recipe_shop_active", columnList = "shop_id, is_active"),
        @Index(name = "idx_recipe_variant", columnList = "variant_id, shop_id"),
        @Index(name = "idx_recipe_status", columnList = "status"),
        @Index(name = "idx_recipe_prep_station", columnList = "prep_station"),
        @Index(name = "idx_recipe_created_at", columnList = "created_at DESC")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
public class Recipe extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @EqualsAndHashCode.Include
    private UUID id;

    @Column(name = "shop_id", nullable = false)
    private UUID shopId;

    // The finished product variant this recipe produces (e.g., "Chapati")
    @Column(name = "variant_id", nullable = false)
    private UUID variantId;

    @Column(name = "recipe_name", nullable = false, length = 200)
    private String recipeName;

    @Column(name = "description", length = 1000)
    private String description;

    // How many units of the finished product this recipe yields
    // e.g., 1kg flour → yields 20 chapatis
    @Column(name = "yield_quantity", nullable = false, precision = 12, scale = 3)
    private BigDecimal yieldQuantity;

    // Unit of the yield (e.g., "pieces", "portions", "servings")
    @Column(name = "yield_unit", length = 50)
    private String yieldUnit;

    // Estimated preparation time in minutes
    @Column(name = "prep_time_minutes")
    private Integer prepTimeMinutes;

    // Estimated cooking time in minutes
    @Column(name = "cook_time_minutes")
    private Integer cookTimeMinutes;

    @Enumerated(EnumType.STRING)
    @Column(name = "prep_station", length = 30)
    private PrepStation prepStation;

    @Enumerated(EnumType.STRING)
    @Column(name = "production_type", nullable = false, length = 20)
    @Builder.Default
    private ProductionType productionType = ProductionType.COOK_TO_ORDER;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private RecipeStatus status = RecipeStatus.DRAFT;

    // Cost per single unit of finished product (auto-calculated from ingredients)
    @Column(name = "cost_per_unit", precision = 12, scale = 2)
    private BigDecimal costPerUnit;

    @Column(name = "instructions", columnDefinition = "TEXT")
    private String instructions;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @OneToMany(mappedBy = "recipe", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<RecipeIngredient> ingredients = new ArrayList<>();
}
