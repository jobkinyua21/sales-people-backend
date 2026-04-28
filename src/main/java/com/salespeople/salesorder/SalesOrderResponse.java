package com.salespeople.salesorder;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

@Data
@Builder
public class SalesOrderResponse {

    private Long salesOrderHeaderId;
    private Long salesOrderNumber;
    private String saleOrderType;
    private Integer salesPersonNumber;
    private SalesOrderStatus status;
    private LocalDate salesOrderDate;
    private OffsetDateTime entryDate;
    private String customerName;
    private String phoneNumber;
    private String customerId;
    private Integer numberOfItems;
    private BigDecimal salesOrderTotalValue;
    private BigDecimal discount;
    private BigDecimal total;
    private String batchNumber;
    private String createdBy;
    private List<SalesOrderLineResponse> lines;
}
