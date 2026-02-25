package com.possystem.sales;

import com.fasterxml.jackson.annotation.JsonInclude;
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
public class SalesPaymentResponse {

    private UUID id;
    private PaymentMethod paymentMethod;
    private BigDecimal amount;
    private String referenceNumber;
    private String notes;
}
