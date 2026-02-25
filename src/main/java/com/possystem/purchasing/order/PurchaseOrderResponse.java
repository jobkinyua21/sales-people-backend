package com.possystem.purchasing.order;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.possystem.purchasing.enums.PurchaseOrderStatus;
import com.possystem.sales.DiscountType;
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
public class PurchaseOrderResponse {

    private UUID id;
    private String poNumber;
    private UUID supplierId;
    private String supplierName;
    private PurchaseOrderStatus orderStatus;
    private LocalDate orderDate;
    private LocalDate expectedDate;
    private BigDecimal subtotal;
    private BigDecimal taxRate;
    private BigDecimal taxAmount;
    private DiscountType discountType;
    private BigDecimal discountValue;
    private BigDecimal discountAmount;
    private BigDecimal totalAmount;
    private String referenceNumber;
    private String notes;
    private UUID orderedBy;
    private LocalDateTime orderedAt;
    private LocalDateTime completedAt;
    private LocalDateTime cancelledAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<PurchaseOrderItemResponse> items;
}
