package com.salespeople.batchorder;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

@Data
@Builder
public class SalesPersonBatchOrderResponse {

    private Long batchOrderId;
    private String batchRef;
    private Long salesOrderHeaderId;
    private Integer salesPersonNumber;
    private LocalDate orderDate;
    private SalesPersonBatchOrderStatus status;
    private String reviewedBy;
    private OffsetDateTime reviewedAt;
    private String rejectionReason;
    private String createdBy;
    private OffsetDateTime createdAt;
    private BigDecimal orderLimit;
    private BigDecimal batchTotal;
}
