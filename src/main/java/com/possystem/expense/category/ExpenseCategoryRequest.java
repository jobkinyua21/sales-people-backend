package com.possystem.expense.category;

import com.possystem.expense.enums.ExpenseCategoryStatus;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.UUID;

@Data
public class ExpenseCategoryRequest {

    private UUID id;

    @NotBlank(message = "Category name is required")
    private String categoryName;

    private String description;

    private ExpenseCategoryStatus status;
}
