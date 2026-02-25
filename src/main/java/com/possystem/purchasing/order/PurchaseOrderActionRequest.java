package com.possystem.purchasing.order;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class PurchaseOrderActionRequest {

    @NotNull(message = "Purchase order ID is required")
    private UUID id;
}
