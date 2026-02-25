package com.possystem.purchasing.grn;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class GrnActionRequest {

    @NotNull(message = "GRN ID is required")
    private UUID id;
}
