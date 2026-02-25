package com.possystem.purchasing.order;

import com.possystem.sales.DiscountType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
public class PurchaseOrderRequest {

    private UUID id;

    @NotNull(message = "Supplier is required")
    private UUID supplierId;

    private LocalDate orderDate;

    private LocalDate expectedDate;

    private BigDecimal taxRate;

    private DiscountType discountType;

    private BigDecimal discountValue;

    private String referenceNumber;

    private String notes;

    @NotEmpty(message = "Purchase order must have at least one item")
    @Valid
    private List<PurchaseOrderItemRequest> items;
}
