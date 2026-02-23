package com.possystem.inventory;

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
public class InventoryStockRequest {

    private UUID id;

    @NotNull(message = "Variant ID is required")
    private UUID variantId;

    private BigDecimal currentQuantity;
    private BigDecimal reorderLevel;
    private BigDecimal reorderQuantity;
}
