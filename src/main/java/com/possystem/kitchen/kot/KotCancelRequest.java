package com.possystem.kitchen.kot;

import jakarta.validation.constraints.NotBlank;
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
public class KotCancelRequest {

    @NotNull(message = "KOT ID is required")
    private UUID kotId;

    @NotBlank(message = "Cancel reason is required")
    private String reason;
}
