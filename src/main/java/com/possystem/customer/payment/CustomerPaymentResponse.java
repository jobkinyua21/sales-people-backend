package com.possystem.customer.payment;

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
public class CustomerPaymentResponse {

    private UUID id;
    private UUID customerId;
    private String customerName;
    private String receiptNumber;
    private PaymentMethod paymentMethod;
    private BigDecimal amount;
    private String referenceNumber;
    private String notes;
    private UUID receivedBy;
    private LocalDateTime receivedAt;
    private LocalDateTime createdAt;
}
