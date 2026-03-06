package com.possystem.purchasing.invoice;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class SupplierInvoiceActionRequest {

    @NotNull(message = "Invoice ID is required")
    private UUID id;
}
