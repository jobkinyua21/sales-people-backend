package com.possystem.mpesa;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MpesaStatusResponse {

    private UUID transactionId;
    private UUID orderId;
    private String phoneNumber;
    private BigDecimal amount;
    private String checkoutRequestId;
    private String mpesaReceiptNumber;
    private MpesaTransactionStatus status;
    private Integer resultCode;
    private String resultDescription;
    private LocalDateTime completedAt;
    private LocalDateTime createdAt;
}
