package com.possystem.purchasing.grn;

import com.fasterxml.jackson.annotation.JsonInclude;
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
public class GrnItemResponse {

    private UUID id;
    private UUID purchaseOrderItemId;
    private UUID variantId;
    private String productName;
    private String variantName;
    private String sku;
    private BigDecimal quantityOrdered;
    private BigDecimal quantityReceived;
    private BigDecimal quantityDamaged;
    private BigDecimal quantityMissing;
    private BigDecimal quantityReturned;
    private BigDecimal unitCost;
    private BigDecimal totalCost;
    private String notes;
}
