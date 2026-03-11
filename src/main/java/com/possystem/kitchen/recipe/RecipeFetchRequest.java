package com.possystem.kitchen.recipe;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecipeFetchRequest {

    private UUID id;
    private String search;
    private RecipeStatus status;
    private PrepStation prepStation;
    private UUID variantId;
    private Integer start;
    private Integer limit;
}
