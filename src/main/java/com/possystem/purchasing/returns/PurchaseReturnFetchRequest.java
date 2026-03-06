package com.possystem.purchasing.returns;

import com.possystem.purchasing.enums.PurchaseReturnStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PurchaseReturnFetchRequest {

    private UUID id;
    private String search;
    @Builder.Default
    private int start = 0;
    private Integer limit;

    private PurchaseReturnStatus returnStatus;
    private UUID supplierId;
    private UUID grnId;
    private UUID purchaseOrderId;
    private LocalDateTime dateFrom;
    private LocalDateTime dateTo;
}
