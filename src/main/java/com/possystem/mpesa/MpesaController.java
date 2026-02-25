package com.possystem.mpesa;

import com.possystem.common.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@Slf4j
public class MpesaController {

    private final MpesaService mpesaService;

    /**
     * Initiate STK push — called by the cashier/frontend (authenticated).
     */
    @PostMapping("/api/v1/mpesa/stkpush")
    public ResponseEntity<ApiResponse<StkPushResponse>> initiateStkPush(
            @Valid @RequestBody StkPushRequest request) {
        StkPushResponse response = mpesaService.initiateStkPush(request);
        return ResponseEntity.ok(ApiResponse.success(response, "STK push initiated. Check your phone."));
    }

    /**
     * Check M-Pesa transaction status.
     * Pass checkoutRequestId or orderId in the request body.
     * Frontend polls this after initiating STK push.
     */
    @PostMapping("/api/v1/mpesa/status")
    public ResponseEntity<ApiResponse<MpesaStatusResponse>> checkStatus(
            @RequestBody MpesaStatusRequest request) {
        MpesaStatusResponse response;
        if (request.getCheckoutRequestId() != null) {
            response = mpesaService.checkTransactionStatus(request.getCheckoutRequestId());
        } else if (request.getOrderId() != null) {
            response = mpesaService.checkOrderPaymentStatus(request.getOrderId());
        } else {
            throw new IllegalArgumentException("Either checkoutRequestId or orderId is required");
        }
        return ResponseEntity.ok(ApiResponse.success(response, "Transaction status retrieved"));
    }

    /**
     * M-Pesa callback — called by Safaricom (no auth required).
     * This endpoint must be publicly accessible.
     */
    @PostMapping("/api/v1/public/mpesa/callback")
    public ResponseEntity<Map<String, String>> mpesaCallback(@RequestBody Map<String, Object> callbackData) {
        log.info("M-Pesa callback received");
        mpesaService.processCallback(callbackData);
        return ResponseEntity.ok(Map.of("ResultCode", "0", "ResultDesc", "Success"));
    }
}
