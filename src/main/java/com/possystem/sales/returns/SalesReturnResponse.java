package com.possystem.sales.returns;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SalesReturnResponse {

    private UUID id;
    private UUID shopId;
    private String returnNumber;
    private UUID orderId;
    private String orderNumber;
    private ReturnStatus status;
    private ReturnReason returnReason;
    private RefundMethod refundMethod;
    private BigDecimal totalRefundAmount;
    private String notes;
    private UUID requestedBy;
    private String requestedByName;
    private UUID approvedBy;
    private String approvedByName;
    private LocalDateTime approvedAt;
    private UUID rejectedBy;
    private LocalDateTime rejectedAt;
    private String rejectionReason;
    private LocalDateTime completedAt;
    private LocalDateTime createdAt;
    private List<ReturnItemResponse> items;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReturnItemResponse {
        private UUID id;
        private UUID orderItemId;
        private UUID variantId;
        private String productName;
        private String variantName;
        private String sku;
        private BigDecimal unitPrice;
        private BigDecimal quantityPurchased;
        private BigDecimal quantityReturned;
        private BigDecimal refundAmount;
        private Boolean restock;
    }
}
