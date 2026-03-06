package com.possystem.customer.payment;

import com.possystem.customer.Customer;
import com.possystem.customer.CustomerRepository;
import com.possystem.security.SecurityContextUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CustomerPaymentService {

    private final CustomerPaymentRepository customerPaymentRepository;
    private final CustomerRepository customerRepository;

    @Transactional
    public CustomerPaymentResponse addPayment(CustomerPaymentRequest request) {
        UUID shopId = SecurityContextUtil.getCurrentShopId();

        Customer customer = customerRepository.findByIdAndShopIdAndIsActiveTrue(request.getCustomerId(), shopId)
                .orElseThrow(() -> new IllegalArgumentException("Customer not found"));

        BigDecimal outstanding = customer.getOutstandingBalance() != null ? customer.getOutstandingBalance() : BigDecimal.ZERO;

        if (outstanding.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Customer has no outstanding balance");
        }

        if (request.getAmount().compareTo(outstanding) > 0) {
            throw new IllegalArgumentException(
                    "Payment amount (" + request.getAmount() + ") exceeds outstanding balance (" + outstanding + ")");
        }

        String receiptNumber = generateReceiptNumber(shopId);

        CustomerPayment payment = CustomerPayment.builder()
                .shopId(shopId)
                .customerId(request.getCustomerId())
                .receiptNumber(receiptNumber)
                .paymentMethod(request.getPaymentMethod())
                .amount(request.getAmount())
                .referenceNumber(request.getReferenceNumber())
                .notes(request.getNotes())
                .receivedBy(SecurityContextUtil.getCurrentUserId())
                .receivedAt(LocalDateTime.now())
                .build();

        CustomerPayment saved = customerPaymentRepository.save(payment);

        // Reduce outstanding balance
        customer.setOutstandingBalance(outstanding.subtract(request.getAmount()));
        customerRepository.save(customer);

        return buildResponse(saved, customer.getCustomerName());
    }

    public List<CustomerPaymentResponse> fetchPayments(UUID customerId) {
        UUID shopId = SecurityContextUtil.getCurrentShopId();

        Customer customer = customerRepository.findByIdAndShopIdAndIsActiveTrue(customerId, shopId)
                .orElseThrow(() -> new IllegalArgumentException("Customer not found"));

        return customerPaymentRepository.findByCustomer(shopId, customerId).stream()
                .map(p -> buildResponse(p, customer.getCustomerName()))
                .toList();
    }

    private String generateReceiptNumber(UUID shopId) {
        long count = customerPaymentRepository.countByShopId(shopId);
        String code;
        do {
            count++;
            code = String.format("CR-%05d", count);
        } while (customerPaymentRepository.existsByShopIdAndReceiptNumber(shopId, code));
        return code;
    }

    private CustomerPaymentResponse buildResponse(CustomerPayment payment, String customerName) {
        return CustomerPaymentResponse.builder()
                .id(payment.getId())
                .customerId(payment.getCustomerId())
                .customerName(customerName)
                .receiptNumber(payment.getReceiptNumber())
                .paymentMethod(payment.getPaymentMethod())
                .amount(payment.getAmount())
                .referenceNumber(payment.getReferenceNumber())
                .notes(payment.getNotes())
                .receivedBy(payment.getReceivedBy())
                .receivedAt(payment.getReceivedAt())
                .createdAt(payment.getCreatedAt())
                .build();
    }
}
