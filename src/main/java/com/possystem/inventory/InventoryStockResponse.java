package com.possystem.inventory;

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
public class InventoryStockResponse {

    private UUID id;
    private UUID shopId;
    private UUID variantId;



    // Product & variant info (resolved)
    private UUID productId;
    private String productName;
    private String sku;
    private String categoryName;
    private BigDecimal costPrice;
    private BigDecimal price;
    private String variantName;
    private String productStatus;

    // Stock fields
    private BigDecimal currentQuantity;
    private BigDecimal reorderLevel;
    private BigDecimal reorderQuantity;
    private String stockStatus; // IN_STOCK, LOW_STOCK, OUT_OF_STOCK
    private LocalDateTime lastRestockedAt;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
