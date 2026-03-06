package com.possystem.purchasing.returns;

import com.possystem.purchasing.enums.ReturnReason;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class PurchaseReturnItemRequest {

    @NotNull(message = "Product variant is required")
    private UUID variantId;

    @NotNull(message = "Quantity returned is required")
    @Positive(message = "Quantity returned must be greater than zero")
    private BigDecimal quantityReturned;

    private BigDecimal unitCost;

    private ReturnReason returnReason;

    private String notes;
}
