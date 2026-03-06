package com.possystem.expense;

import com.possystem.sales.PaymentMethod;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data
public class ExpenseRequest {

    private UUID id;

    @NotNull(message = "Expense category is required")
    private UUID expenseCategoryId;

    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be greater than zero")
    private BigDecimal amount;

    private LocalDate expenseDate;

    private PaymentMethod paymentMethod;

    private String vendor;

    private String referenceNumber;

    private String description;

    private String notes;
}
