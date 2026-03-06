package com.possystem.purchasing.returns;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
public class PurchaseReturnRequest {

    private UUID id;

    @NotNull(message = "GRN is required")
    private UUID grnId;

    private LocalDate returnDate;

    private String referenceNumber;

    private String reason;

    private String notes;

    @NotEmpty(message = "Purchase return must have at least one item")
    @Valid
    private List<PurchaseReturnItemRequest> items;
}
