package com.salespeople.item;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Data
@Builder
public class ItemsRegisterResponse {

    private Long itemRegisterId;
    private Integer itemCode;
    private String itemName;
    private String itemUnits;
    private BigDecimal itemUnitsValue;
    private String itemUnitsAbbreviations;
    private BigDecimal previousPrice;
    private BigDecimal currentPrice;
    private Integer accountNumber;
    private BigDecimal constraint;
    private Boolean status;
    private Boolean disabled;
    private OffsetDateTime createdAt;
}
