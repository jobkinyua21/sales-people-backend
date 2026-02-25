package com.possystem.purchasing.grn;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
public class GrnRequest {

    private UUID id;

    @NotNull(message = "Purchase order is required")
    private UUID purchaseOrderId;

    private LocalDate receivedDate;

    private String referenceNumber;

    private String notes;

    @NotEmpty(message = "GRN must have at least one item")
    @Valid
    private List<GrnItemRequest> items;
}
