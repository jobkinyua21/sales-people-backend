package com.possystem.kitchen.kot;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KotUpdateItemStatusRequest {

    @NotNull(message = "KOT item ID is required")
    private UUID itemId;

    @NotNull(message = "Status is required")
    private KotItemStatus status;

    private String cancelReason;
}
