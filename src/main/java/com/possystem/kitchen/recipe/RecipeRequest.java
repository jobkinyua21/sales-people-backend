package com.possystem.kitchen.recipe;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
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
public class RecipeRequest {

    private UUID id;

    @NotNull(message = "Product variant ID is required")
    private UUID variantId;

    @NotBlank(message = "Recipe name is required")
    private String recipeName;

    private String description;

    @NotNull(message = "Yield quantity is required")
    private BigDecimal yieldQuantity;

    private String yieldUnit;

    private Integer prepTimeMinutes;

    private Integer cookTimeMinutes;

    private PrepStation prepStation;

    private ProductionType productionType;

    private String instructions;

    @NotEmpty(message = "At least one ingredient is required")
    @Valid
    private List<RecipeIngredientRequest> ingredients;
}
