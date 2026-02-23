package com.possystem.inventory;

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
public class ProductVariantRequest {

    private UUID id;

    private UUID productId;

    private String variantName;
    private String sku;

    @NotNull(message = "Price is required")
    private BigDecimal price;

    private BigDecimal costPrice;
    private BigDecimal compareAtPrice;
    private String barcode;
    private BigDecimal weight;
    private String uom;
    private Boolean trackStock;
    private Integer sortOrder;
    private ProductVariantStatus status;
}
