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
public class ProductVariantResponse {

    private UUID id;
    private UUID shopId;
    private UUID productId;
    private String sku;
    private String variantName;
    private BigDecimal price;
    private BigDecimal costPrice;
    private BigDecimal compareAtPrice;
    private String barcode;
    private BigDecimal weight;
    private String uom;
    private Boolean trackStock;
    private Boolean isDefault;
    private Integer sortOrder;
    private ProductVariantStatus status;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private InventoryStockResponse stock;
}
