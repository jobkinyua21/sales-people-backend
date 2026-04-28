package com.salespeople.batchorder;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class SalesPersonBatchOrderRequest {

    @NotNull(message = "Order date is required")
    private LocalDate orderDate;
}
