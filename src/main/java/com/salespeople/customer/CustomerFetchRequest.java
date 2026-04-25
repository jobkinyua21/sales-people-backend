package com.salespeople.customer;

import lombok.Data;

@Data
public class CustomerFetchRequest {

    private Long customerId;
    private String search;
    private int start = 0;
    private Integer limit;
}
