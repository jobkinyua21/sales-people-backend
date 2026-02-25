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
public class SalesOrderItemResponse {

    private UUID id;
    private UUID variantId;
    private String productName;
    private String variantName;
    private String sku;
    private BigDecimal unitPrice;
    private BigDecimal costPrice;
    private BigDecimal quantity;
    private DiscountType discountType;
    private BigDecimal discountValue;
    private BigDecimal discountAmount;
    private BigDecimal taxAmount;
    private BigDecimal totalPrice;
}
