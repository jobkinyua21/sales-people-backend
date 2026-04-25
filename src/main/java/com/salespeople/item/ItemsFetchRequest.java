package com.salespeople.item;

import lombok.Data;

@Data
public class ItemsFetchRequest {

    private Long itemRegisterId;
    private Integer itemCode;
    private String search;
    private int start = 0;
    private Integer limit;
}
