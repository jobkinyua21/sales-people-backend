package com.possystem.sales.returns;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RejectReturnRequest {

    @NotBlank(message = "Rejection reason is required")
    private String reason;
}
