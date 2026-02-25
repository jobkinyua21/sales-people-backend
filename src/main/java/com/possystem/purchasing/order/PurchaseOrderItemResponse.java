package com.possystem.purchasing.order;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.possystem.sales.DiscountType;
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
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PurchaseOrderItemResponse {

    private UUID id;
    private UUID variantId;
    private String productName;
    private String variantName;
    private String sku;
    private BigDecimal quantity;
    private BigDecimal receivedQuantity;
    private BigDecimal damagedQuantity;
    private BigDecimal unitCost;
    private DiscountType discountType;
    private BigDecimal discountValue;
    private BigDecimal discountAmount;
    private BigDecimal taxAmount;
    private BigDecimal totalCost;
}
