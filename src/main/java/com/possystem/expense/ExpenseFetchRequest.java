package com.possystem.expense;

import com.possystem.expense.enums.ExpenseStatus;
import com.possystem.sales.PaymentMethod;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExpenseFetchRequest {

    private UUID id;
    private String search;

    @Builder.Default
    private int start = 0;
    private Integer limit;

    private ExpenseStatus expenseStatus;
    private UUID expenseCategoryId;
    private PaymentMethod paymentMethod;
    private LocalDateTime dateFrom;
    private LocalDateTime dateTo;
}
