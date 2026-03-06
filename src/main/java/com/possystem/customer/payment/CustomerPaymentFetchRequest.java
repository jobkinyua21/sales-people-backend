package com.possystem.customer.payment;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class CustomerPaymentFetchRequest {

    @NotNull(message = "Customer ID is required")
    private UUID customerId;
}
