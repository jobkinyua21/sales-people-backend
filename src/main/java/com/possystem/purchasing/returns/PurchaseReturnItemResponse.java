package com.possystem.purchasing.returns;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.possystem.purchasing.enums.ReturnReason;
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
public class PurchaseReturnItemResponse {

    private UUID id;
    private UUID grnItemId;
    private UUID variantId;
    private String productName;
    private String variantName;
    private String sku;
    private BigDecimal quantityReceived;
    private BigDecimal quantityAlreadyReturned;
    private BigDecimal quantityReturned;
    private BigDecimal unitCost;
    private BigDecimal totalCost;
    private ReturnReason returnReason;
    private String notes;
}
