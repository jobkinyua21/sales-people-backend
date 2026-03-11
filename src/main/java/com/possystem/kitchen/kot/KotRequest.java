package com.possystem.kitchen.kot;

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
public class KotRequest {

    @NotNull(message = "Order ID is required")
    private UUID orderId;

    private Integer priority;

    private String specialInstructions;

    @NotEmpty(message = "At least one item is required")
    @Valid
    private List<KotItemRequest> items;
}
