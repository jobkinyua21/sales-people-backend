package com.possystem.supplier;

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
public class SupplierResponse {

    private UUID id;
    private UUID shopId;
    private String supplierCode;
    private String supplierName;
    private String companyName;
    private String contactPerson;
    private String contactNumber;
    private String email;
    private String tinNumber;
    private String physicalAddress;
    private String city;
    private String country;
    private PaymentTerms paymentTerms;
    private String bankName;
    private String bankAccountNumber;
    private String website;
    private String notes;
    private SupplierStatus status;
    private BigDecimal totalPurchases;
    private BigDecimal totalPaid;
    private BigDecimal outstandingBalance;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
