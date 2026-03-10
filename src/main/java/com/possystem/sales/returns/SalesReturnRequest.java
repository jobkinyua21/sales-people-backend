package com.possystem.sales.returns;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SalesReturnRequest {

    @NotNull(message = "Order ID is required")
    private UUID orderId;

    @NotNull(message = "Return reason is required")
    private ReturnReason returnReason;

    @NotNull(message = "Refund method is required")
    private RefundMethod refundMethod;

    @NotEmpty(message = "At least one return item is required")
    @Valid
    private List<SalesReturnItemRequest> items;

    private String notes;
}
