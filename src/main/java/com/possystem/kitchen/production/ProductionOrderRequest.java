package com.possystem.kitchen.production;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductionOrderRequest {

    @NotNull(message = "Recipe ID is required")
    private UUID recipeId;

    @NotNull(message = "Quantity to produce is required")
    private BigDecimal quantityToProduce;

    private String notes;
}
