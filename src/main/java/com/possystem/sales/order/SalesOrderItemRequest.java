package com.possystem.sales;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class SalesOrderItemRequest {

    @NotNull(message = "Variant ID is required")
    private UUID variantId;

    @NotNull(message = "Quantity is required")
    private BigDecimal quantity;

    private DiscountType discountType;

    private BigDecimal discountValue;
}
