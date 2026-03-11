package com.possystem.kitchen.production;

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
public class ProductionOrderResponse {

    private UUID id;
    private UUID shopId;
    private String productionNumber;
    private UUID recipeId;
    private String recipeName;
    private String productName;
    private String variantName;
    private UUID variantId;
    private BigDecimal quantityToProduce;
    private BigDecimal quantityProduced;
    private String yieldUnit;
    private BigDecimal estimatedCost;
    private BigDecimal actualCost;
    private BigDecimal costPerUnit;
    private ProductionStatus status;
    private String notes;
    private UUID requestedBy;
    private String requestedByName;
    private UUID producedBy;
    private String producedByName;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private LocalDateTime cancelledAt;
    private String cancelReason;
    private Boolean ingredientsDeducted;
    private Boolean stockAdded;
    private LocalDateTime createdAt;

    // Variance tracking
    private BigDecimal yieldVariance; // quantityProduced - quantityToProduce
    private BigDecimal yieldPercentage; // (quantityProduced / quantityToProduce) * 100

    // Ingredient breakdown for this production run
    private List<ProductionIngredient> ingredients;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProductionIngredient {
        private String ingredientName;
        private BigDecimal quantityUsed;
        private String unitOfMeasure;
        private BigDecimal unitCost;
        private BigDecimal totalCost;
    }
}
