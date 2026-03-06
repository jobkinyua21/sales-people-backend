package com.possystem.purchasing.returns;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.possystem.purchasing.enums.PurchaseReturnStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PurchaseReturnResponse {

    private UUID id;
    private String returnNumber;
    private UUID supplierId;
    private String supplierName;
    private UUID grnId;
    private String grnNumber;
    private UUID purchaseOrderId;
    private String poNumber;
    private PurchaseReturnStatus returnStatus;
    private LocalDate returnDate;
    private UUID returnedBy;
    private String referenceNumber;
    private String reason;
    private String notes;
    private BigDecimal totalAmount;
    private LocalDateTime submittedAt;
    private LocalDateTime completedAt;
    private LocalDateTime cancelledAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<PurchaseReturnItemResponse> items;
}
