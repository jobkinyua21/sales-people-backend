package com.possystem.purchasing.grn;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.possystem.purchasing.enums.GrnStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GrnResponse {

    private UUID id;
    private String grnNumber;
    private UUID purchaseOrderId;
    private String poNumber;
    private String supplierName;
    private GrnStatus grnStatus;
    private LocalDate receivedDate;
    private UUID receivedBy;
    private String referenceNumber;
    private String notes;
    private LocalDateTime completedAt;
    private LocalDateTime cancelledAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<GrnItemResponse> items;
}
