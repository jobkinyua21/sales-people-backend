package com.possystem.purchasing.invoice;

import com.possystem.purchasing.enums.SupplierInvoiceStatus;
import com.possystem.sales.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SupplierInvoiceFetchRequest {

    private UUID id;
    private String search;
    @Builder.Default
    private int start = 0;
    private Integer limit;

    private SupplierInvoiceStatus invoiceStatus;
    private PaymentStatus paymentStatus;
    private UUID supplierId;
    private LocalDate dueDateFrom;
    private LocalDate dueDateTo;
    private LocalDateTime dateFrom;
    private LocalDateTime dateTo;
    private Boolean overdueOnly;
}
