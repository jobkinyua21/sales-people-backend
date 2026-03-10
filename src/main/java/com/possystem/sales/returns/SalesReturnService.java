package com.possystem.sales.returns;

import com.possystem.auth.user.User;
import com.possystem.auth.user.UserRepository;
import com.possystem.customer.CustomerRepository;
import com.possystem.inventory.InventoryStock;
import com.possystem.inventory.InventoryStockRepository;
import com.possystem.inventory.stockalert.StockAlertService;
import com.possystem.sales.OrderStatus;
import com.possystem.sales.PaymentMethod;
import com.possystem.sales.SalesOrder;
import com.possystem.sales.SalesOrderItem;
import com.possystem.sales.SalesOrderRepository;
import com.possystem.sales.register.CashRegisterService;
import com.possystem.security.SecurityContextUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SalesReturnService {

    private final SalesReturnRepository salesReturnRepository;
    private final SalesOrderRepository salesOrderRepository;
    private final InventoryStockRepository inventoryStockRepository;
    private final CustomerRepository customerRepository;
    private final UserRepository userRepository;
    private final CashRegisterService cashRegisterService;
    private final StockAlertService stockAlertService;

    // ==================== CREATE RETURN REQUEST ====================

    @Transactional
    public SalesReturnResponse createReturn(SalesReturnRequest request) {
        UUID shopId = SecurityContextUtil.getCurrentShopId();
        UUID userId = SecurityContextUtil.getCurrentUserId();

        SalesOrder order = salesOrderRepository.findByIdAndShopIdAndIsActiveTrue(request.getOrderId(), shopId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));

        if (order.getOrderStatus() != OrderStatus.COMPLETED) {
            throw new IllegalArgumentException("Returns can only be created for COMPLETED orders");
        }

        // Build return items and validate quantities
        List<SalesReturnItem> returnItems = new ArrayList<>();
        BigDecimal totalRefund = BigDecimal.ZERO;

        for (SalesReturnItemRequest itemReq : request.getItems()) {
            SalesOrderItem orderItem = order.getItems().stream()
                    .filter(i -> i.getId().equals(itemReq.getOrderItemId()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Order item not found: " + itemReq.getOrderItemId()));

            if (itemReq.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Return quantity must be greater than zero");
            }

            // Check already returned quantity for this item
            BigDecimal alreadyReturned = salesReturnRepository.totalReturnedQuantity(order.getId(), orderItem.getId());
            BigDecimal maxReturnable = orderItem.getQuantity().subtract(alreadyReturned);

            if (itemReq.getQuantity().compareTo(maxReturnable) > 0) {
                throw new IllegalArgumentException(
                        "Cannot return " + itemReq.getQuantity() + " of " + orderItem.getProductName() +
                        ". Maximum returnable: " + maxReturnable +
                        " (purchased: " + orderItem.getQuantity() + ", already returned: " + alreadyReturned + ")");
            }

            // Calculate refund for this item (proportional to quantity)
            BigDecimal itemRefund = orderItem.getUnitPrice().multiply(itemReq.getQuantity());

            SalesReturnItem returnItem = SalesReturnItem.builder()
                    .orderItemId(orderItem.getId())
                    .variantId(orderItem.getVariantId())
                    .productName(orderItem.getProductName())
                    .variantName(orderItem.getVariantName())
                    .sku(orderItem.getSku())
                    .unitPrice(orderItem.getUnitPrice())
                    .quantityPurchased(orderItem.getQuantity())
                    .quantityReturned(itemReq.getQuantity())
                    .refundAmount(itemRefund)
                    .restock(itemReq.getRestock() != null ? itemReq.getRestock() : true)
                    .build();

            returnItems.add(returnItem);
            totalRefund = totalRefund.add(itemRefund);
        }

        // Validate refund method
        if (request.getRefundMethod() == RefundMethod.CASH) {
            UUID currentUserId = SecurityContextUtil.getCurrentUserId();
            if (!cashRegisterService.hasOpenSession(shopId, currentUserId)) {
                throw new IllegalArgumentException("You need an open register session to process cash refunds");
            }
        }

        if (request.getRefundMethod() == RefundMethod.STORE_CREDIT && order.getCustomerId() == null) {
            throw new IllegalArgumentException("Store credit refund requires a customer on the original order");
        }

        // Create the return
        SalesReturn salesReturn = SalesReturn.builder()
                .shopId(shopId)
                .returnNumber(generateReturnNumber(shopId))
                .orderId(order.getId())
                .orderNumber(order.getOrderNumber())
                .status(ReturnStatus.PENDING)
                .returnReason(request.getReturnReason())
                .refundMethod(request.getRefundMethod())
                .totalRefundAmount(totalRefund)
                .notes(request.getNotes())
                .requestedBy(userId)
                .build();

        // Link items
        for (SalesReturnItem item : returnItems) {
            item.setSalesReturn(salesReturn);
        }
        salesReturn.setItems(returnItems);

        SalesReturn saved = salesReturnRepository.save(salesReturn);
        return buildResponse(saved);
    }

    // ==================== APPROVAL ====================

    @Transactional
    public SalesReturnResponse approveReturn(UUID returnId) {
        UUID shopId = SecurityContextUtil.getCurrentShopId();
        UUID userId = SecurityContextUtil.getCurrentUserId();

        SalesReturn salesReturn = salesReturnRepository.findByIdAndShopId(returnId, shopId)
                .orElseThrow(() -> new IllegalArgumentException("Return not found"));

        if (salesReturn.getStatus() != ReturnStatus.PENDING) {
            throw new IllegalArgumentException("Only PENDING returns can be approved");
        }

        salesReturn.setStatus(ReturnStatus.APPROVED);
        salesReturn.setApprovedBy(userId);
        salesReturn.setApprovedAt(LocalDateTime.now());

        // Process the refund and restock
        processRefund(salesReturn, shopId);

        salesReturn.setStatus(ReturnStatus.COMPLETED);
        salesReturn.setCompletedAt(LocalDateTime.now());

        SalesReturn saved = salesReturnRepository.save(salesReturn);
        return buildResponse(saved);
    }

    @Transactional
    public SalesReturnResponse rejectReturn(UUID returnId, RejectReturnRequest request) {
        UUID shopId = SecurityContextUtil.getCurrentShopId();
        UUID userId = SecurityContextUtil.getCurrentUserId();

        SalesReturn salesReturn = salesReturnRepository.findByIdAndShopId(returnId, shopId)
                .orElseThrow(() -> new IllegalArgumentException("Return not found"));

        if (salesReturn.getStatus() != ReturnStatus.PENDING) {
            throw new IllegalArgumentException("Only PENDING returns can be rejected");
        }

        salesReturn.setStatus(ReturnStatus.REJECTED);
        salesReturn.setRejectedBy(userId);
        salesReturn.setRejectedAt(LocalDateTime.now());
        salesReturn.setRejectionReason(request.getReason());

        SalesReturn saved = salesReturnRepository.save(salesReturn);
        return buildResponse(saved);
    }

    // ==================== QUERIES ====================

    public List<SalesReturnResponse> getAllReturns() {
        UUID shopId = SecurityContextUtil.getCurrentShopId();
        return salesReturnRepository.findAllByShopIdOrderByCreatedAtDesc(shopId).stream()
                .map(this::buildResponse)
                .toList();
    }

    public List<SalesReturnResponse> getPendingReturns() {
        UUID shopId = SecurityContextUtil.getCurrentShopId();
        return salesReturnRepository.findAllByShopIdAndStatusOrderByCreatedAtDesc(shopId, ReturnStatus.PENDING).stream()
                .map(this::buildResponse)
                .toList();
    }

    public List<SalesReturnResponse> getReturnsByOrder(UUID orderId) {
        UUID shopId = SecurityContextUtil.getCurrentShopId();
        return salesReturnRepository.findAllByOrderIdAndShopId(orderId, shopId).stream()
                .map(this::buildResponse)
                .toList();
    }

    // ==================== REFUND PROCESSING ====================

    private void processRefund(SalesReturn salesReturn, UUID shopId) {
        UUID approverId = salesReturn.getApprovedBy();

        // 1. Restock items that are marked for restocking
        for (SalesReturnItem item : salesReturn.getItems()) {
            if (Boolean.TRUE.equals(item.getRestock())) {
                inventoryStockRepository.findByVariantIdAndShopIdAndIsActiveTrue(item.getVariantId(), shopId)
                        .ifPresent(stock -> {
                            stock.setCurrentQuantity(stock.getCurrentQuantity().add(item.getQuantityReturned()));
                            stock.setLastRestockedAt(LocalDateTime.now());
                            inventoryStockRepository.save(stock);

                            // Resolve any stock alerts
                            stockAlertService.checkAndResolveAlert(shopId, item.getVariantId());
                        });
            }
        }

        // 2. Process refund based on method
        switch (salesReturn.getRefundMethod()) {
            case CASH -> {
                // Record cash refund in the approver's register session
                cashRegisterService.recordCashRefund(shopId, approverId, salesReturn.getTotalRefundAmount());
            }
            case STORE_CREDIT -> {
                // Add refund amount to customer's credit balance
                SalesOrder order = salesOrderRepository.findById(salesReturn.getOrderId()).orElse(null);
                if (order != null && order.getCustomerId() != null) {
                    customerRepository.findByIdAndShopIdAndIsActiveTrue(order.getCustomerId(), shopId)
                            .ifPresent(customer -> {
                                BigDecimal outstanding = customer.getOutstandingBalance() != null
                                        ? customer.getOutstandingBalance() : BigDecimal.ZERO;
                                customer.setOutstandingBalance(outstanding.subtract(salesReturn.getTotalRefundAmount()).max(BigDecimal.ZERO));
                                customerRepository.save(customer);
                            });
                }
            }
            case ORIGINAL_PAYMENT -> {
                // For card/mobile money, the refund is processed externally
                // Just record it for tracking — no cash register impact
            }
        }
    }

    // ==================== RESPONSE BUILDING ====================

    private SalesReturnResponse buildResponse(SalesReturn salesReturn) {
        List<SalesReturnResponse.ReturnItemResponse> items = salesReturn.getItems().stream()
                .map(item -> SalesReturnResponse.ReturnItemResponse.builder()
                        .id(item.getId())
                        .orderItemId(item.getOrderItemId())
                        .variantId(item.getVariantId())
                        .productName(item.getProductName())
                        .variantName(item.getVariantName())
                        .sku(item.getSku())
                        .unitPrice(item.getUnitPrice())
                        .quantityPurchased(item.getQuantityPurchased())
                        .quantityReturned(item.getQuantityReturned())
                        .refundAmount(item.getRefundAmount())
                        .restock(item.getRestock())
                        .build())
                .toList();

        SalesReturnResponse response = SalesReturnResponse.builder()
                .id(salesReturn.getId())
                .shopId(salesReturn.getShopId())
                .returnNumber(salesReturn.getReturnNumber())
                .orderId(salesReturn.getOrderId())
                .orderNumber(salesReturn.getOrderNumber())
                .status(salesReturn.getStatus())
                .returnReason(salesReturn.getReturnReason())
                .refundMethod(salesReturn.getRefundMethod())
                .totalRefundAmount(salesReturn.getTotalRefundAmount())
                .notes(salesReturn.getNotes())
                .requestedBy(salesReturn.getRequestedBy())
                .approvedBy(salesReturn.getApprovedBy())
                .approvedAt(salesReturn.getApprovedAt())
                .rejectedBy(salesReturn.getRejectedBy())
                .rejectedAt(salesReturn.getRejectedAt())
                .rejectionReason(salesReturn.getRejectionReason())
                .completedAt(salesReturn.getCompletedAt())
                .createdAt(salesReturn.getCreatedAt())
                .items(items)
                .build();

        // Resolve names
        response.setRequestedByName(resolveName(salesReturn.getRequestedBy()));
        if (salesReturn.getApprovedBy() != null) {
            response.setApprovedByName(resolveName(salesReturn.getApprovedBy()));
        }

        return response;
    }

    // ==================== HELPERS ====================

    private String resolveName(UUID userId) {
        if (userId == null) return null;
        return userRepository.findById(userId)
                .map(User::getFullName)
                .orElse(null);
    }

    private String generateReturnNumber(UUID shopId) {
        List<String> existingNumbers = salesReturnRepository.findAllByShopIdOrderByCreatedAtDesc(shopId).stream()
                .map(SalesReturn::getReturnNumber)
                .toList();

        long count = salesReturnRepository.countByShopId(shopId);
        String code;
        do {
            count++;
            code = String.format("RET-%04d", count);
        } while (existingNumbers.contains(code));
        return code;
    }
}
