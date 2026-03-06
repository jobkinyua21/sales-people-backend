package com.possystem.expense;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class ExpenseActionRequest {

    @NotNull(message = "Expense ID is required")
    private UUID id;

    private String rejectionReason;
}
