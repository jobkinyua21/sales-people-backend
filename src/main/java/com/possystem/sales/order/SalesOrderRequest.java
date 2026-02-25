package com.possystem.sales;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
public class SalesOrderRequest {

    private UUID id;

    private UUID customerId;

    private BigDecimal taxRate;

    private DiscountType discountType;

    private BigDecimal discountValue;

    private String referenceNumber;

    private String notes;

    @NotEmpty(message = "Order must have at least one item")
    @Valid
    private List<SalesOrderItemRequest> items;

    private List<@Valid SalesPaymentRequest> payments;
}
