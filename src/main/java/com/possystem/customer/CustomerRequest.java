package com.possystem.customer;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class CustomerRequest {

    private UUID id;

    @NotBlank(message = "Customer name is required")
    private String customerName;

    private String contactNumber;

    private String email;

    private CustomerGender gender;

    private BigDecimal balanceCredit;

    private String tinNumber;

    private String physicalAddress;

    private CustomerStatus status;
}
