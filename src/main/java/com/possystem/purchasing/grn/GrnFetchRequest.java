package com.possystem.purchasing.grn;

import com.possystem.purchasing.enums.GrnStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GrnFetchRequest {

    private UUID id;
    private String search;
    @Builder.Default
    private int start = 0;
    private Integer limit;

    private GrnStatus grnStatus;
    private UUID supplierId;
    private UUID purchaseOrderId;
    private LocalDateTime dateFrom;
    private LocalDateTime dateTo;
}
