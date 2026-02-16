package com.possystem.module;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.possystem.module.enums.ModuleStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AdditionalModuleResponse {

    private UUID id;
    private String moduleCode;
    private String moduleName;
    private String description;
    private BigDecimal monthlyPrice;
    private BigDecimal yearlyPrice;
    private String currency;
    private ModuleStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
