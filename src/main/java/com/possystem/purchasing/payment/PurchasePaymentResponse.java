package com.possystem.purchasing.payment;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.possystem.sales.PaymentMethod;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PurchasePaymentResponse {

    private UUID id;
    private String voucherNumber;
    private PaymentMethod paymentMethod;
    private BigDecimal amount;
    private String referenceNumber;
    private String notes;
    private UUID paidBy;
    private LocalDateTime paidAt;
    private String receiptUrl;
    private LocalDateTime createdAt;
}
