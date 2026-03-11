package com.possystem.kitchen.production;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductionCancelRequest {

    @NotNull(message = "Production order ID is required")
    private UUID productionOrderId;

    private String reason;
}
