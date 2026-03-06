package com.possystem.purchasing.invoice;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.possystem.purchasing.enums.SupplierInvoiceStatus;
import com.possystem.sales.PaymentStatus;
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
public class SupplierInvoiceSummaryResponse {

    private UUID id;
    private String invoiceNumber;
    private SupplierInvoiceStatus invoiceStatus;
    private PaymentStatus paymentStatus;
    private BigDecimal totalAmount;
    private BigDecimal amountPaid;
}
