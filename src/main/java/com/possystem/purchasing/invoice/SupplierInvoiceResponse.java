package com.possystem.purchasing.invoice;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.possystem.purchasing.enums.SupplierInvoiceStatus;
import com.possystem.sales.PaymentStatus;
import com.possystem.supplier.PaymentTerms;
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
public class SupplierInvoiceResponse {

    private UUID id;
    private String invoiceNumber;
    private String referenceCode;
    private UUID supplierId;
    private String supplierName;
    private SupplierInvoiceStatus invoiceStatus;
    private PaymentStatus paymentStatus;
    private LocalDate invoiceDate;
    private LocalDate receivedDate;
    private LocalDate dueDate;
    private PaymentTerms paymentTerms;
    private BigDecimal subtotal;
    private BigDecimal taxAmount;
    private BigDecimal totalAmount;
    private BigDecimal amountPaid;
    private BigDecimal balanceDue;
    private Boolean isOverdue;
    private String notes;
    private UUID approvedBy;
    private LocalDateTime approvedAt;
    private LocalDateTime cancelledAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<SupplierInvoicePurchaseOrderResponse> purchaseOrders;
}
