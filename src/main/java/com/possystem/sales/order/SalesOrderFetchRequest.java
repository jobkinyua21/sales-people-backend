package com.possystem.sales;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SalesOrderFetchRequest {

    private UUID id;
    private String search;
    @Builder.Default
    private int start = 0;
    private Integer limit;

    // Domain-specific filters
    private OrderStatus orderStatus;
    private PaymentStatus paymentStatus;
    private PaymentMethod paymentMethod;
    private UUID customerId;
    private LocalDateTime dateFrom;
    private LocalDateTime dateTo;
}
