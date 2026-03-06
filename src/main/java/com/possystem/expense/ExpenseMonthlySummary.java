package com.possystem.expense;

import java.math.BigDecimal;

public interface ExpenseMonthlySummary {
    Integer getYear();
    Integer getMonth();
    BigDecimal getTotal();
}
