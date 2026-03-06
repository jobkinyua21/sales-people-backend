package com.possystem.mpesa;

import com.possystem.sales.AddPaymentRequest;
import com.possystem.sales.PaymentMethod;
import com.possystem.sales.SalesOrder;
import com.possystem.sales.SalesOrderRepository;
import com.possystem.sales.SalesOrderService;
import com.possystem.security.SecurityContextUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class MpesaService {

    private final MpesaConfig mpesaConfig;
    private final MpesaTransactionRepository mpesaTransactionRepository;
    private final SalesOrderRepository salesOrderRepository;
    private final SalesOrderService salesOrderService;
    private final RestTemplate restTemplate;

    // ==================== STK PUSH ====================

    @Transactional
    public StkPushResponse initiateStkPush(StkPushRequest request) {
        UUID shopId = SecurityContextUtil.getCurrentShopId();

        // Validate order exists
        SalesOrder order = salesOrderRepository.findByIdAndShopIdAndIsActiveTrue(request.getOrderId(), shopId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));

        // Format phone number (ensure 254 prefix)
        String phone = formatPhoneNumber(request.getPhoneNumber());

        // Get OAuth token
        String accessToken = getAccessToken();

        // Generate timestamp and password
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String password = Base64.getEncoder().encodeToString(
                (mpesaConfig.getShortCode() + mpesaConfig.getPassKey() + timestamp)
                        .getBytes(StandardCharsets.UTF_8));

        // Build STK push payload
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("BusinessShortCode", mpesaConfig.getShortCode());
        payload.put("Password", password);
        payload.put("Timestamp", timestamp);
        payload.put("TransactionType", "CustomerPayBillOnline");
        payload.put("Amount", request.getAmount().intValue());
        payload.put("PartyA", phone);
        payload.put("PartyB", mpesaConfig.getShortCode());
        payload.put("PhoneNumber", phone);
        payload.put("CallBackURL", mpesaConfig.getCallbackUrl());
        payload.put("AccountReference", order.getOrderNumber());
        payload.put("TransactionDesc", "Payment for " + order.getOrderNumber());

        // Send STK push request
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(accessToken);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);

        try {
            ResponseEntity<StkPushResponse> response = restTemplate.exchange(
                    mpesaConfig.getBaseUrl() + "/mpesa/stkpush/v1/processrequest",
                    HttpMethod.POST,
                    entity,
                    StkPushResponse.class);

            StkPushResponse stkResponse = response.getBody();

            if (stkResponse != null && "0".equals(stkResponse.getResponseCode())) {
                // Save transaction record
                MpesaTransaction transaction = MpesaTransaction.builder()
                        .shopId(shopId)
                        .orderId(request.getOrderId())
                        .phoneNumber(phone)
                        .amount(request.getAmount())
                        .merchantRequestId(stkResponse.getMerchantRequestId())
                        .checkoutRequestId(stkResponse.getCheckoutRequestId())
                        .status(MpesaTransactionStatus.PENDING)
                        .build();
                mpesaTransactionRepository.save(transaction);

                log.info("STK push initiated for order {} — CheckoutRequestID: {}",
                        order.getOrderNumber(), stkResponse.getCheckoutRequestId());
            }

            return stkResponse;

        } catch (Exception e) {
            log.error("STK push failed for order {}: {}", order.getOrderNumber(), e.getMessage());
            throw new IllegalArgumentException("M-Pesa STK push failed: " + e.getMessage());
        }
    }

    // ==================== CALLBACK PROCESSING ====================

    @Transactional
    public void processCallback(Map<String, Object> callbackData) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> body = (Map<String, Object>) callbackData.get("Body");
            @SuppressWarnings("unchecked")
            Map<String, Object> stkCallback = (Map<String, Object>) body.get("stkCallback");

            String merchantRequestId = (String) stkCallback.get("MerchantRequestID");
            String checkoutRequestId = (String) stkCallback.get("CheckoutRequestID");
            Integer resultCode = (Integer) stkCallback.get("ResultCode");
            String resultDesc = (String) stkCallback.get("ResultDesc");

            log.info("M-Pesa callback received — CheckoutRequestID: {}, ResultCode: {}", checkoutRequestId, resultCode);

            MpesaTransaction transaction = mpesaTransactionRepository
                    .findByCheckoutRequestId(checkoutRequestId)
                    .orElse(null);

            if (transaction == null) {
                log.warn("No transaction found for CheckoutRequestID: {}", checkoutRequestId);
                return;
            }

            transaction.setResultCode(resultCode);
            transaction.setResultDescription(resultDesc);

            if (resultCode == 0) {
                // Payment successful — extract receipt number
                String mpesaReceiptNumber = extractReceiptNumber(stkCallback);
                transaction.setMpesaReceiptNumber(mpesaReceiptNumber);
                transaction.setStatus(MpesaTransactionStatus.COMPLETED);
                transaction.setCompletedAt(LocalDateTime.now());

                // Add payment to the sales order
                AddPaymentRequest paymentRequest = new AddPaymentRequest();
                paymentRequest.setOrderId(transaction.getOrderId());
                paymentRequest.setPaymentMethod(PaymentMethod.MOBILE_MONEY);
                paymentRequest.setAmount(transaction.getAmount());
                paymentRequest.setReferenceNumber(mpesaReceiptNumber);
                paymentRequest.setNotes("M-Pesa payment — " + transaction.getPhoneNumber());
                salesOrderService.addPaymentInternal(paymentRequest);

                log.info("M-Pesa payment confirmed for order {} — Receipt: {}",
                        transaction.getOrderId(), mpesaReceiptNumber);
            } else {
                transaction.setStatus(resultCode == 1032
                        ? MpesaTransactionStatus.CANCELLED
                        : MpesaTransactionStatus.FAILED);
                log.info("M-Pesa payment failed/cancelled for order {} — ResultCode: {}, Desc: {}",
                        transaction.getOrderId(), resultCode, resultDesc);
            }

            mpesaTransactionRepository.save(transaction);

        } catch (Exception e) {
            log.error("Error processing M-Pesa callback: {}", e.getMessage(), e);
        }
    }

    // ==================== STATUS CHECK ====================

    public MpesaStatusResponse checkTransactionStatus(String checkoutRequestId) {
        UUID shopId = SecurityContextUtil.getCurrentShopId();
        MpesaTransaction transaction = mpesaTransactionRepository
                .findByCheckoutRequestIdAndShopId(checkoutRequestId, shopId)
                .orElseThrow(() -> new IllegalArgumentException("Transaction not found"));

        return buildStatusResponse(transaction);
    }

    public MpesaStatusResponse checkOrderPaymentStatus(UUID orderId) {
        UUID shopId = SecurityContextUtil.getCurrentShopId();
        MpesaTransaction transaction = mpesaTransactionRepository
                .findTopByOrderIdAndShopIdOrderByCreatedAtDesc(orderId, shopId)
                .orElseThrow(() -> new IllegalArgumentException("No M-Pesa transaction found for this order"));

        return buildStatusResponse(transaction);
    }

    private MpesaStatusResponse buildStatusResponse(MpesaTransaction transaction) {
        return MpesaStatusResponse.builder()
                .transactionId(transaction.getId())
                .orderId(transaction.getOrderId())
                .phoneNumber(transaction.getPhoneNumber())
                .amount(transaction.getAmount())
                .checkoutRequestId(transaction.getCheckoutRequestId())
                .mpesaReceiptNumber(transaction.getMpesaReceiptNumber())
                .status(transaction.getStatus())
                .resultCode(transaction.getResultCode())
                .resultDescription(transaction.getResultDescription())
                .completedAt(transaction.getCompletedAt())
                .createdAt(transaction.getCreatedAt())
                .build();
    }

    // ==================== OAUTH TOKEN ====================

    private String getAccessToken() {
        String credentials = mpesaConfig.getConsumerKey() + ":" + mpesaConfig.getConsumerSecret();
        String encodedCredentials = Base64.getEncoder().encodeToString(
                credentials.getBytes(StandardCharsets.UTF_8));

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Basic " + encodedCredentials);

        HttpEntity<String> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    mpesaConfig.getBaseUrl() + "/oauth/v1/generate?grant_type=client_credentials",
                    HttpMethod.GET,
                    entity,
                    Map.class);

            Map<String, Object> body = response.getBody();
            if (body != null && body.containsKey("access_token")) {
                return (String) body.get("access_token");
            }
            throw new IllegalArgumentException("Failed to get M-Pesa access token");

        } catch (Exception e) {
            log.error("M-Pesa OAuth failed: {}", e.getMessage());
            throw new IllegalArgumentException("M-Pesa authentication failed: " + e.getMessage());
        }
    }

    // ==================== HELPERS ====================

    private String formatPhoneNumber(String phone) {
        phone = phone.replaceAll("[^0-9]", "");
        if (phone.startsWith("0")) {
            phone = "254" + phone.substring(1);
        } else if (phone.startsWith("+254")) {
            phone = phone.substring(1);
        } else if (!phone.startsWith("254")) {
            phone = "254" + phone;
        }
        return phone;
    }

    @SuppressWarnings("unchecked")
    private String extractReceiptNumber(Map<String, Object> stkCallback) {
        Map<String, Object> callbackMetadata = (Map<String, Object>) stkCallback.get("CallbackMetadata");
        if (callbackMetadata == null) return null;

        List<Map<String, Object>> items = (List<Map<String, Object>>) callbackMetadata.get("Item");
        if (items == null) return null;

        for (Map<String, Object> item : items) {
            if ("MpesaReceiptNumber".equals(item.get("Name"))) {
                return (String) item.get("Value");
            }
        }
        return null;
    }

}
