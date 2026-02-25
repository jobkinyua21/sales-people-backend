package com.possystem.supplier;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.UUID;

@Data
public class SupplierRequest {

    private UUID id;

    @NotBlank(message = "Supplier name is required")
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
}
