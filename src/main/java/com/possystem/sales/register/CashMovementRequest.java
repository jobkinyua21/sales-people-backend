package com.possystem.sales.register;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CashMovementRequest {

    @NotNull(message = "Movement type is required")
    private CashMovementType movementType;

    @NotNull(message = "Amount is required")
    private BigDecimal amount;

    @NotBlank(message = "Reason is required")
    private String reason;
}
