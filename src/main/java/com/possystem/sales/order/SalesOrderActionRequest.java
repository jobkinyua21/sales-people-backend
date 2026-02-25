package com.possystem.sales;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class SalesOrderActionRequest {

    @NotNull(message = "Order ID is required")
    private UUID id;
}
