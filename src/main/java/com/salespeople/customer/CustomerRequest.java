package com.salespeople.customer;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CustomerRequest {

    private Long customerId;

    @NotBlank(message = "Outlet name is required")
    private String customerOutletName;

    @NotBlank(message = "Contact person is required")
    private String customerContactPerson;

    @NotBlank(message = "Contact number is required")
    private String customerContact;

    private String customerEmail;
    private String customerLocation;
    private Boolean buyOnCredit;
}
