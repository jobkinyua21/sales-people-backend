package com.salespeople.salesorder;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class SalesOrderRequest {

    private Long salesOrderHeaderId; // if present, update existing order

    @NotNull(message = "Customer ID is required")
    private Long customerId;

    @NotNull(message = "Sales order date is required")
    private LocalDate salesOrderDate;

    @NotBlank(message = "Order type is required")
    private String saleOrderType;

    @NotEmpty(message = "At least one item is required")
    @Valid
    private List<SalesOrderLineRequest> lines;
}
