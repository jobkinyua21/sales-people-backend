package com.possystem.kitchen.recipe;

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
public class RecipeIngredientRequest {

    @NotNull(message = "Ingredient variant ID is required")
    private UUID ingredientVariantId;

    @NotNull(message = "Quantity required is required")
    private BigDecimal quantityRequired;

    private String unitOfMeasure;

    private BigDecimal wastePercentage;

    private Boolean isOptional;

    private String notes;
}
