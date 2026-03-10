package com.possystem.inventory.stockalert;

import com.fasterxml.jackson.annotation.JsonInclude;
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
public class StockAlertResponse {

    private UUID id;
    private UUID shopId;
    private UUID variantId;
    private String productName;
    private String variantName;
    private String sku;
    private StockAlertType alertType;
    private StockAlertStatus status;
    private BigDecimal currentQuantity;
    private BigDecimal reorderLevel;
    private UUID acknowledgedBy;
    private LocalDateTime acknowledgedAt;
    private LocalDateTime resolvedAt;
    private LocalDateTime createdAt;
}
