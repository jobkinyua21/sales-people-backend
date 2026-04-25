package com.salespeople.discount;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class DiscountSetupResponse {

    private Long discountSetupId;
    private Integer itemCode;
    private BigDecimal discountStartValue;
    private BigDecimal discountValue;
    private LocalDateTime createdAt;
}
