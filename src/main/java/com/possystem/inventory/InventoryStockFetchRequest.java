package com.possystem.inventory;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryStockFetchRequest {

    private UUID id;
    private String search;

    @Builder.Default
    private int start = 0;

    private Integer limit;

    // Filters
    private UUID categoryId;
    private String stockStatus; // IN_STOCK, LOW_STOCK, OUT_OF_STOCK
}
