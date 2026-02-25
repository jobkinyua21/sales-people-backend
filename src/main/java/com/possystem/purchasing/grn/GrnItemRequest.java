package com.possystem.purchasing.grn;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class GrnItemRequest {

    @NotNull(message = "Purchase order item ID is required")
    private UUID purchaseOrderItemId;

    @NotNull(message = "Quantity received is required")
    @PositiveOrZero(message = "Quantity received cannot be negative")
    private BigDecimal quantityReceived;

    @PositiveOrZero(message = "Damaged quantity cannot be negative")
    private BigDecimal quantityDamaged;

    @PositiveOrZero(message = "Missing quantity cannot be negative")
    private BigDecimal quantityMissing;

    private BigDecimal unitCost;

    private String notes;
}
