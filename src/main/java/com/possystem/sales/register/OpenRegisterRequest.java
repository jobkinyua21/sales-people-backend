package com.possystem.sales.register;

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
public class OpenRegisterRequest {

    @NotNull(message = "Opening balance is required")
    private BigDecimal openingBalance;

    private String notes;
}
