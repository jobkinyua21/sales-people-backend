package com.possystem.sales.register;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CashRegisterSessionResponse {

    private UUID id;
    private UUID shopId;
    private UUID openedBy;
    private UUID closedBy;
    private RegisterSessionStatus status;
    private BigDecimal openingBalance;
    private BigDecimal totalCashSales;
    private BigDecimal totalCashIn;
    private BigDecimal totalCashOut;
    private BigDecimal totalCashRefunds;
    private BigDecimal expectedClosingBalance;
    private BigDecimal actualClosingBalance;
    private BigDecimal difference;
    private String notes;
    private LocalDateTime openedAt;
    private LocalDateTime closedAt;
    private LocalDateTime createdAt;
    private List<CashMovementResponse> movements;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CashMovementResponse {
        private UUID id;
        private CashMovementType movementType;
        private BigDecimal amount;
        private String reason;
        private UUID recordedBy;
        private LocalDateTime createdAt;
    }
}
