package com.possystem.kitchen.kot;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KotItemRequest {

    @NotNull(message = "Order item ID is required")
    private UUID orderItemId;

    @NotNull(message = "Quantity is required")
    private BigDecimal quantity;

    private String specialInstructions;
}
