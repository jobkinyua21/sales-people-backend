package com.possystem.purchasing.order;

import com.possystem.sales.DiscountType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class PurchaseOrderItemRequest {

    @NotNull(message = "Variant is required")
    private UUID variantId;

    @NotNull(message = "Quantity is required")
    @Positive(message = "Quantity must be greater than zero")
    private BigDecimal quantity;

    @NotNull(message = "Unit cost is required")
    @Positive(message = "Unit cost must be greater than zero")
    private BigDecimal unitCost;

    private DiscountType discountType;

    private BigDecimal discountValue;
}
