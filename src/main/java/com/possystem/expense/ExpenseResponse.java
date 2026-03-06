package com.possystem.expense;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.possystem.expense.enums.ExpenseStatus;
import com.possystem.sales.PaymentMethod;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ExpenseResponse {

    private UUID id;
    private String expenseNumber;
    private UUID expenseCategoryId;
    private String categoryName;
    private BigDecimal amount;
    private LocalDate expenseDate;
    private PaymentMethod paymentMethod;
    private ExpenseStatus expenseStatus;
    private String vendor;
    private String referenceNumber;
    private String description;
    private String notes;
    private UUID recordedBy;
    private UUID approvedBy;
    private LocalDateTime approvedAt;
    private UUID rejectedBy;
    private LocalDateTime rejectedAt;
    private String rejectionReason;
    private LocalDateTime paidAt;
    private LocalDateTime cancelledAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
