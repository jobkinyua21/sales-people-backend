package com.possystem.expense;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ExpenseSummaryResponse {

    private BigDecimal totalExpenses;
    private List<MonthlyTotal> monthlyTotals;
    private List<CategoryBreakdown> categoryBreakdowns;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MonthlyTotal {
        private int year;
        private int month;
        private BigDecimal total;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CategoryBreakdown {
        private UUID categoryId;
        private String categoryName;
        private BigDecimal total;
        private List<MonthlyTotal> monthlyTotals;
    }
}
