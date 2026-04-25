package com.salespeople.salesorder;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class SalesOrderLineResponse {

    private Long orderLineId;
    private Integer itemCode;
    private String itemName;
    private Integer accountNumber;
    private String accountName;
    private Integer storeCode;
    private String storeName;
    private Long quantity;
    private BigDecimal costPerItem;
    private BigDecimal discountStartValue;
    private BigDecimal discountValue;
    private BigDecimal subTotal;
    private BigDecimal total;
    private String status;
}
