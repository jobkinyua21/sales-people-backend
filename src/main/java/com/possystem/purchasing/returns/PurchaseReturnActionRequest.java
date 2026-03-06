package com.possystem.purchasing.returns;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class PurchaseReturnActionRequest {

    @NotNull(message = "Purchase return ID is required")
    private UUID id;
}
