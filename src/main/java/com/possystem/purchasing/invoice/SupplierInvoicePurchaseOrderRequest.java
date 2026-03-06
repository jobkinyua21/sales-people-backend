package com.possystem.purchasing.invoice;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class SupplierInvoicePurchaseOrderRequest {

    @NotNull(message = "Purchase order ID is required")
    private UUID purchaseOrderId;

    @NotNull(message = "Allocated amount is required")
    private BigDecimal allocatedAmount;
}
