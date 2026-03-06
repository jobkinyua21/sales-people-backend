package com.possystem.purchasing.invoice;

import com.possystem.supplier.PaymentTerms;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
public class SupplierInvoiceRequest {

    private UUID id;

    @NotBlank(message = "Invoice number is required")
    private String invoiceNumber;

    @NotNull(message = "Supplier is required")
    private UUID supplierId;

    private LocalDate invoiceDate;
    private LocalDate receivedDate;
    private LocalDate dueDate;
    private PaymentTerms paymentTerms;

    private BigDecimal subtotal;
    private BigDecimal taxAmount;

    @NotNull(message = "Total amount is required")
    private BigDecimal totalAmount;

    private String notes;

    @NotNull(message = "At least one purchase order is required")
    private List<SupplierInvoicePurchaseOrderRequest> purchaseOrders;
}
