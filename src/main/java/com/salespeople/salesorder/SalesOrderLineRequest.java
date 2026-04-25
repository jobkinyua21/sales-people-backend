package com.salespeople.salesorder;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SalesOrderLineRequest {

    @NotNull(message = "Item code is required")
    private Integer itemCode;

    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be at least 1")
    private Long quantity;

    @NotNull(message = "Store code is required")
    private Integer storeCode;
}
