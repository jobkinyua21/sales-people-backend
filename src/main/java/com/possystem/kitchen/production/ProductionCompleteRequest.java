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
public class ProductionCompleteRequest {

    @NotNull(message = "Production order ID is required")
    private UUID productionOrderId;

    // Actual quantity produced (may differ from planned — e.g., got 195 chapatis instead of 200)
    @NotNull(message = "Actual quantity produced is required")
    private BigDecimal quantityProduced;

    private String notes;
}
