package com.salespeople.customer;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Data
@Builder
public class CustomerResponse {

    private Long customerId;
    private String customerOutletName;
    private String customerContactPerson;
    private String customerContact;
    private String customerEmail;
    private String customerLocation;
    private String fullName;
    private Boolean buyOnCredit;
    private BigDecimal balance;
    private OffsetDateTime createdAt;
}
