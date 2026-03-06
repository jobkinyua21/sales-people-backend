package com.possystem.expense.category;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.possystem.expense.enums.ExpenseCategoryStatus;
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
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ExpenseCategoryResponse {

    private UUID id;
    private UUID shopId;
    private String categoryCode;
    private String categoryName;
    private String description;
    private ExpenseCategoryStatus status;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
