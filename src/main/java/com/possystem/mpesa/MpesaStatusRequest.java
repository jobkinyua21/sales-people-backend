package com.possystem.mpesa;

import lombok.Data;

import java.util.UUID;

@Data
public class MpesaStatusRequest {

    private String checkoutRequestId;

    private UUID orderId;
}
