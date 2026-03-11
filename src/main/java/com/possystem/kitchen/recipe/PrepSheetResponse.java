package com.possystem.kitchen.recipe;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PrepSheetResponse {

    private UUID recipeId;
    private String recipeName;
    private String productName;
    private String variantName;
    private BigDecimal quantityToPrepare;
    private BigDecimal recipeYield;
    private String yieldUnit;
    private Integer estimatedPrepMinutes;
    private Integer estimatedCookMinutes;
    private PrepStation prepStation;
    private String instructions;
    private List<PrepIngredient> ingredients;
    private boolean allIngredientsAvailable;
    private List<String> insufficientIngredients;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PrepIngredient {
        private UUID ingredientVariantId;
        private String ingredientName;
        private String variantName;
        private BigDecimal quantityNeeded;     // scaled to the quantity being prepared
        private BigDecimal wasteAllowance;     // extra quantity for waste
        private BigDecimal totalQuantityNeeded; // quantityNeeded + wasteAllowance
        private String unitOfMeasure;
        private BigDecimal currentStock;
        private boolean sufficient;            // currentStock >= totalQuantityNeeded
        private boolean optional;
        private String notes;
    }
}
