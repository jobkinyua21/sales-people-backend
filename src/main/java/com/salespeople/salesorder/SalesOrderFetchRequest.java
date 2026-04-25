package com.salespeople.salesorder;

import lombok.Data;

@Data
public class SalesOrderFetchRequest {

    private Long salesOrderHeaderId;
    private Long salesOrderNumber;
    private Integer salesPersonNumber;
    private String status;
    private String search;
    private int start = 0;
    private Integer limit;
}
