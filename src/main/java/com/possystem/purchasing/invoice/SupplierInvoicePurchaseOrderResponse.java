package com.possystem.purchasing.invoice;

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
public class SupplierInvoicePurchaseOrderResponse {

    private UUID purchaseOrderId;
    private String poNumber;
    private BigDecimal allocatedAmount;
}
