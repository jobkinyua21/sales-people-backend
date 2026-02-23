package com.possystem.customer;

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
public class CustomerResponse {

    private UUID id;
    private UUID shopId;
    private String customerCode;
    private String customerName;
    private String contactNumber;
    private String email;
    private CustomerGender gender;
    private BigDecimal balanceCredit;
    private String tinNumber;
    private String physicalAddress;
    private BigDecimal totalPurchases;
    private LocalDateTime lastPurchaseDate;
    private CustomerStatus status;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
