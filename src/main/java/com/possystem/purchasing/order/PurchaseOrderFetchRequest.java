package com.possystem.purchasing.order;

import com.possystem.purchasing.enums.PurchaseOrderStatus;
import com.possystem.sales.PaymentStatus;
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
public class PurchaseOrderFetchRequest {

    private UUID id;
    private String search;
    @Builder.Default
    private int start = 0;
    private Integer limit;

    private PurchaseOrderStatus orderStatus;
    private PaymentStatus paymentStatus;
    private UUID supplierId;
    private LocalDateTime dateFrom;
    private LocalDateTime dateTo;
}
