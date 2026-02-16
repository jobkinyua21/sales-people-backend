package com.possystem.module;

import com.possystem.module.enums.ModuleStatus;
import jakarta.validation.constraints.NotBlank;
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
public class AdditionalModuleRequest {

    private UUID id;

    @NotBlank(message = "Module name is required")
    private String moduleName;

    private String description;

    @NotNull(message = "Monthly price is required")
    private BigDecimal monthlyPrice;

    private BigDecimal yearlyPrice;

    private ModuleStatus status;
}
