package com.possystem.expense;

import java.math.BigDecimal;
import java.util.UUID;

public interface ExpenseCategorySummary {
    Integer getYear();
    Integer getMonth();
    UUID getCategoryId();
    String getCategoryName();
    BigDecimal getTotal();
}
