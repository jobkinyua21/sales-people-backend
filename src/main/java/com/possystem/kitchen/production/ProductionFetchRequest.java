package com.possystem.kitchen.production;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductionFetchRequest {

    private UUID id;
    private String search;
    private ProductionStatus status;
    private UUID recipeId;
    private Integer start;
    private Integer limit;
}
