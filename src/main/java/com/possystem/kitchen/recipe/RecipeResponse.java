package com.possystem.kitchen.recipe;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RecipeResponse {

    private UUID id;
    private UUID shopId;
    private UUID variantId;
    private String productName;
    private String variantName;
    private String recipeName;
    private String description;
    private BigDecimal yieldQuantity;
    private String yieldUnit;
    private Integer prepTimeMinutes;
    private Integer cookTimeMinutes;
    private PrepStation prepStation;
    private ProductionType productionType;
    private RecipeStatus status;
    private BigDecimal costPerUnit;
    private BigDecimal totalRecipeCost;
    private String instructions;
    private LocalDateTime createdAt;
    private List<RecipeIngredientResponse> ingredients;

    // Yield analysis
    private BigDecimal ingredientCostPerUnit;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecipeIngredientResponse {
        private UUID id;
        private UUID ingredientVariantId;
        private String ingredientName;
        private String variantName;
        private String sku;
        private BigDecimal quantityRequired;
        private String unitOfMeasure;
        private BigDecimal unitCost;
        private BigDecimal totalCost;
        private BigDecimal wastePercentage;
        private BigDecimal effectiveQuantity; // quantityRequired adjusted for waste
        private Boolean isOptional;
        private String notes;
        private BigDecimal currentStock; // live stock level
    }
}
