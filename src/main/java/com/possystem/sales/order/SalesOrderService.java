package com.possystem.sales;

import com.possystem.common.ListResponse;
import com.possystem.customer.Customer;
import com.possystem.customer.CustomerRepository;
import com.possystem.inventory.InventoryStock;
import com.possystem.inventory.InventoryStockRepository;
import com.possystem.inventory.Product;
import com.possystem.inventory.ProductRepository;
import com.possystem.inventory.ProductVariant;
import com.possystem.inventory.ProductVariantRepository;
import com.possystem.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SalesOrderService {

    private final SalesOrderRepository salesOrderRepository;
    private final ProductVariantRepository productVariantRepository;
    private final ProductRepository productRepository;
    private final InventoryStockRepository inventoryStockRepository;
    private final CustomerRepository customerRepository;
    private final ModelMapper modelMapper;

    // ==================== CRUD ====================

    @Transactional
    public SalesOrderResponse save(SalesOrderRequest request) {
        UUID shopId = getCurrentShopId();

        if (request.getId() != null) {
            return updateOrder(request, shopId);
        }
        return createOrder(request, shopId);
    }

    public ListResponse<SalesOrderResponse> fetch(SalesOrderFetchRequest request) {
        UUID shopId = getCurrentShopId();

        if (request.getId() != null) {
            SalesOrder order = salesOrderRepository.findByIdAndShopIdAndIsActiveTrue(request.getId(), shopId)
                    .orElseThrow(() -> new IllegalArgumentException("Order not found"));
            List<SalesOrderResponse> result = List.of(buildOrderResponse(order, shopId));
            return ListResponse.of(result);
        }

        String search = request.getSearch();
        String orderStatus = request.getOrderStatus() != null ? request.getOrderStatus().name() : null;
        String paymentStatus = request.getPaymentStatus() != null ? request.getPaymentStatus().name() : null;
        String paymentMethod = request.getPaymentMethod() != null ? request.getPaymentMethod().name() : null;
        UUID customerId = request.getCustomerId();
        LocalDateTime dateFrom = request.getDateFrom();
        LocalDateTime dateTo = request.getDateTo();
        Integer limit = request.getLimit();

        if (limit == null) {
            List<SalesOrder> all = salesOrderRepository.searchFiltered(
                    shopId, search, orderStatus, paymentStatus, paymentMethod, customerId, dateFrom, dateTo);
            List<SalesOrderResponse> responses = all.stream()
                    .map(o -> buildOrderResponse(o, shopId))
                    .toList();
            return ListResponse.of(responses);
        }

        PageRequest pageRequest = PageRequest.of(request.getStart(), limit);
        Page<SalesOrder> page = salesOrderRepository.searchFiltered(
                shopId, search, orderStatus, paymentStatus, paymentMethod, customerId, dateFrom, dateTo, pageRequest);
        Page<SalesOrderResponse> responsePage = page.map(o -> buildOrderResponse(o, shopId));
        return ListResponse.from(responsePage);
    }

    @Transactional
    public SalesOrderResponse completeOrder(UUID orderId) {
        UUID shopId = getCurrentShopId();
        SalesOrder order = salesOrderRepository.findByIdAndShopIdAndIsActiveTrue(orderId, shopId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));

        if (order.getOrderStatus() != OrderStatus.PENDING) {
            throw new IllegalArgumentException("Only PENDING orders can be completed");
        }

        if (order.getPaymentStatus() != PaymentStatus.PAID && order.getPaymentStatus() != PaymentStatus.OVERPAID) {
            throw new IllegalArgumentException("Order must be fully paid before completion");
        }

        // Deduct stock (idempotency guard)
        if (!order.getStockDeducted()) {
            deductStock(order, shopId);
            order.setStockDeducted(true);
        }

        // Handle credit payments — deduct from customer balance
        handleCreditPayments(order, shopId);

        // Update customer stats
        updateCustomerStats(order, shopId);

        order.setOrderStatus(OrderStatus.COMPLETED);
        order.setCompletedAt(LocalDateTime.now());

        SalesOrder saved = salesOrderRepository.save(order);
        return buildOrderResponse(saved, shopId);
    }

    @Transactional
    public SalesOrderResponse cancelOrder(UUID orderId) {
        UUID shopId = getCurrentShopId();
        SalesOrder order = salesOrderRepository.findByIdAndShopIdAndIsActiveTrue(orderId, shopId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));

        if (order.getOrderStatus() != OrderStatus.PENDING && order.getOrderStatus() != OrderStatus.COMPLETED) {
            throw new IllegalArgumentException("Only PENDING or COMPLETED orders can be cancelled");
        }

        // Restore stock if it was deducted
        if (order.getStockDeducted()) {
            restoreStock(order, shopId);
            order.setStockDeducted(false);
        }

        // Reverse customer stats if order was completed
        if (order.getOrderStatus() == OrderStatus.COMPLETED) {
            reverseCustomerStats(order, shopId);
            restoreCreditPayments(order, shopId);
        }

        order.setOrderStatus(OrderStatus.CANCELLED);
        order.setCancelledAt(LocalDateTime.now());

        SalesOrder saved = salesOrderRepository.save(order);
        return buildOrderResponse(saved, shopId);
    }

    @Transactional
    public SalesOrderResponse addPayment(AddPaymentRequest request) {
        UUID shopId = getCurrentShopId();
        SalesOrder order = salesOrderRepository.findByIdAndShopIdAndIsActiveTrue(request.getOrderId(), shopId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));

        if (order.getOrderStatus() != OrderStatus.PENDING) {
            throw new IllegalArgumentException("Payments can only be added to PENDING orders");
        }

        SalesPayment payment = SalesPayment.builder()
                .salesOrder(order)
                .paymentMethod(request.getPaymentMethod())
                .amount(request.getAmount())
                .referenceNumber(request.getReferenceNumber())
                .notes(request.getNotes())
                .build();

        order.getPayments().add(payment);
        recalculatePaymentStatus(order);

        SalesOrder saved = salesOrderRepository.save(order);
        return buildOrderResponse(saved, shopId);
    }

    @Transactional
    public void delete(UUID id) {
        UUID shopId = getCurrentShopId();
        SalesOrder order = salesOrderRepository.findByIdAndShopIdAndIsActiveTrue(id, shopId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));

        if (order.getOrderStatus() != OrderStatus.PENDING && order.getOrderStatus() != OrderStatus.CANCELLED) {
            throw new IllegalArgumentException("Only PENDING or CANCELLED orders can be deleted");
        }

        order.setIsActive(false);
        salesOrderRepository.save(order);
    }

    @Transactional
    public int bulkDelete(List<UUID> ids) {
        UUID shopId = getCurrentShopId();
        List<SalesOrder> orders = salesOrderRepository.findAllByIdInAndShopIdAndIsActiveTrue(ids, shopId);
        if (orders.isEmpty()) {
            throw new IllegalArgumentException("No orders found for the given IDs");
        }

        for (SalesOrder order : orders) {
            if (order.getOrderStatus() != OrderStatus.PENDING && order.getOrderStatus() != OrderStatus.CANCELLED) {
                throw new IllegalArgumentException("Order " + order.getOrderNumber() + " cannot be deleted — only PENDING or CANCELLED orders");
            }
            order.setIsActive(false);
        }
        salesOrderRepository.saveAll(orders);
        return orders.size();
    }

    // ==================== CREATE / UPDATE ====================

    private SalesOrderResponse createOrder(SalesOrderRequest request, UUID shopId) {
        SalesOrder order = new SalesOrder();
        order.setShopId(shopId);
        order.setOrderNumber(generateOrderNumber(shopId));
        order.setCustomerId(request.getCustomerId());
        order.setTaxRate(request.getTaxRate() != null ? request.getTaxRate() : BigDecimal.ZERO);
        order.setDiscountType(request.getDiscountType());
        order.setDiscountValue(request.getDiscountValue());
        order.setReferenceNumber(request.getReferenceNumber());
        order.setNotes(request.getNotes());
        order.setServedBy(getCurrentUserId());
        order.setOrderStatus(OrderStatus.PENDING);
        order.setPaymentStatus(PaymentStatus.UNPAID);
        order.setStockDeducted(false);
        order.setIsActive(true);

        // Build order items with snapshots
        List<SalesOrderItem> items = new ArrayList<>();
        for (SalesOrderItemRequest itemReq : request.getItems()) {
            SalesOrderItem item = buildOrderItem(itemReq, order, shopId);
            items.add(item);
        }
        order.setItems(items);

        // Compute totals
        computeOrderTotals(order);

        // Build payments if provided
        if (request.getPayments() != null && !request.getPayments().isEmpty()) {
            List<SalesPayment> payments = new ArrayList<>();
            for (SalesPaymentRequest paymentReq : request.getPayments()) {
                SalesPayment payment = SalesPayment.builder()
                        .salesOrder(order)
                        .paymentMethod(paymentReq.getPaymentMethod())
                        .amount(paymentReq.getAmount())
                        .referenceNumber(paymentReq.getReferenceNumber())
                        .notes(paymentReq.getNotes())
                        .build();
                payments.add(payment);
            }
            order.setPayments(payments);
            recalculatePaymentStatus(order);
        }

        SalesOrder saved = salesOrderRepository.save(order);
        return buildOrderResponse(saved, shopId);
    }

    private SalesOrderResponse updateOrder(SalesOrderRequest request, UUID shopId) {
        SalesOrder order = salesOrderRepository.findByIdAndShopIdAndIsActiveTrue(request.getId(), shopId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));

        if (order.getOrderStatus() != OrderStatus.PENDING) {
            throw new IllegalArgumentException("Only PENDING orders can be updated");
        }

        order.setCustomerId(request.getCustomerId());
        order.setTaxRate(request.getTaxRate() != null ? request.getTaxRate() : order.getTaxRate());
        order.setDiscountType(request.getDiscountType());
        order.setDiscountValue(request.getDiscountValue());
        order.setReferenceNumber(request.getReferenceNumber());
        order.setNotes(request.getNotes());

        // Rebuild items
        order.getItems().clear();
        List<SalesOrderItem> items = new ArrayList<>();
        for (SalesOrderItemRequest itemReq : request.getItems()) {
            SalesOrderItem item = buildOrderItem(itemReq, order, shopId);
            items.add(item);
        }
        order.getItems().addAll(items);

        // Recompute totals
        computeOrderTotals(order);

        // Rebuild payments
        if (request.getPayments() != null) {
            order.getPayments().clear();
            List<SalesPayment> payments = new ArrayList<>();
            for (SalesPaymentRequest paymentReq : request.getPayments()) {
                SalesPayment payment = SalesPayment.builder()
                        .salesOrder(order)
                        .paymentMethod(paymentReq.getPaymentMethod())
                        .amount(paymentReq.getAmount())
                        .referenceNumber(paymentReq.getReferenceNumber())
                        .notes(paymentReq.getNotes())
                        .build();
                payments.add(payment);
            }
            order.getPayments().addAll(payments);
        }
        recalculatePaymentStatus(order);

        SalesOrder saved = salesOrderRepository.save(order);
        return buildOrderResponse(saved, shopId);
    }

    // ==================== ITEM BUILDING ====================

    private SalesOrderItem buildOrderItem(SalesOrderItemRequest itemReq, SalesOrder order, UUID shopId) {
        ProductVariant variant = productVariantRepository.findByIdAndShopIdAndIsActiveTrue(itemReq.getVariantId(), shopId)
                .orElseThrow(() -> new IllegalArgumentException("Product variant not found: " + itemReq.getVariantId()));

        Product product = productRepository.findByIdAndShopIdAndIsActiveTrue(variant.getProductId(), shopId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found for variant: " + variant.getId()));

        // Validate stock availability
        if (Boolean.TRUE.equals(variant.getTrackStock())) {
            InventoryStock stock = inventoryStockRepository
                    .findByVariantIdAndShopIdAndIsActiveTrue(variant.getId(), shopId)
                    .orElseThrow(() -> new IllegalArgumentException(
                            "No stock record found for " + product.getProductName() + " (" + variant.getSku() + ")"));

            if (stock.getCurrentQuantity().compareTo(itemReq.getQuantity()) < 0) {
                throw new IllegalArgumentException(
                        "Insufficient stock for " + product.getProductName() +
                        " (" + variant.getSku() + "). Available: " + stock.getCurrentQuantity() +
                        ", Requested: " + itemReq.getQuantity());
            }
        }

        BigDecimal unitPrice = variant.getPrice();
        BigDecimal costPrice = variant.getCostPrice();
        BigDecimal quantity = itemReq.getQuantity();

        // Compute item-level discount
        BigDecimal lineTotal = unitPrice.multiply(quantity);
        BigDecimal itemDiscount = computeDiscount(lineTotal, itemReq.getDiscountType(), itemReq.getDiscountValue());

        BigDecimal totalPrice = lineTotal.subtract(itemDiscount);

        return SalesOrderItem.builder()
                .salesOrder(order)
                .variantId(variant.getId())
                .productName(product.getProductName())
                .variantName(variant.getVariantName())
                .sku(variant.getSku())
                .unitPrice(unitPrice)
                .costPrice(costPrice)
                .quantity(quantity)
                .discountType(itemReq.getDiscountType())
                .discountValue(itemReq.getDiscountValue())
                .discountAmount(itemDiscount)
                .taxAmount(BigDecimal.ZERO)
                .totalPrice(totalPrice)
                .build();
    }

    // ==================== COMPUTATION ====================

    private void computeOrderTotals(SalesOrder order) {
        // Subtotal = sum of all item totals (after item discounts, before tax)
        BigDecimal subtotal = order.getItems().stream()
                .map(SalesOrderItem::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        order.setSubtotal(subtotal);

        // Order-level discount
        BigDecimal orderDiscount = computeDiscount(subtotal, order.getDiscountType(), order.getDiscountValue());
        order.setDiscountAmount(orderDiscount);

        BigDecimal afterDiscount = subtotal.subtract(orderDiscount);

        // Tax
        BigDecimal taxRate = order.getTaxRate() != null ? order.getTaxRate() : BigDecimal.ZERO;
        BigDecimal taxAmount = afterDiscount.multiply(taxRate)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        order.setTaxAmount(taxAmount);

        // Distribute tax across items proportionally
        distributeTax(order, taxAmount, afterDiscount);

        // Total
        BigDecimal totalAmount = afterDiscount.add(taxAmount);
        order.setTotalAmount(totalAmount);
    }

    private void distributeTax(SalesOrder order, BigDecimal totalTax, BigDecimal afterDiscount) {
        if (afterDiscount.compareTo(BigDecimal.ZERO) == 0 || totalTax.compareTo(BigDecimal.ZERO) == 0) {
            order.getItems().forEach(item -> item.setTaxAmount(BigDecimal.ZERO));
            return;
        }

        BigDecimal distributed = BigDecimal.ZERO;
        List<SalesOrderItem> items = order.getItems();
        for (int i = 0; i < items.size(); i++) {
            SalesOrderItem item = items.get(i);
            if (i == items.size() - 1) {
                // Last item gets the remainder to avoid rounding errors
                item.setTaxAmount(totalTax.subtract(distributed));
            } else {
                BigDecimal proportion = item.getTotalPrice().divide(afterDiscount, 10, RoundingMode.HALF_UP);
                BigDecimal itemTax = totalTax.multiply(proportion).setScale(2, RoundingMode.HALF_UP);
                item.setTaxAmount(itemTax);
                distributed = distributed.add(itemTax);
            }
        }
    }

    private BigDecimal computeDiscount(BigDecimal amount, DiscountType type, BigDecimal value) {
        if (type == null || value == null || value.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        if (type == DiscountType.PERCENTAGE) {
            return amount.multiply(value)
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        }
        // FIXED — cap at the amount to avoid negative totals
        return value.min(amount);
    }

    private void recalculatePaymentStatus(SalesOrder order) {
        BigDecimal amountPaid = order.getPayments().stream()
                .map(SalesPayment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        order.setAmountPaid(amountPaid);

        int comparison = amountPaid.compareTo(order.getTotalAmount());
        if (amountPaid.compareTo(BigDecimal.ZERO) == 0) {
            order.setPaymentStatus(PaymentStatus.UNPAID);
            order.setChangeAmount(BigDecimal.ZERO);
        } else if (comparison < 0) {
            order.setPaymentStatus(PaymentStatus.PARTIAL);
            order.setChangeAmount(BigDecimal.ZERO);
        } else if (comparison == 0) {
            order.setPaymentStatus(PaymentStatus.PAID);
            order.setChangeAmount(BigDecimal.ZERO);
        } else {
            order.setPaymentStatus(PaymentStatus.OVERPAID);
            order.setChangeAmount(amountPaid.subtract(order.getTotalAmount()));
        }
    }

    // ==================== STOCK ====================

    private void deductStock(SalesOrder order, UUID shopId) {
        for (SalesOrderItem item : order.getItems()) {
            ProductVariant variant = productVariantRepository
                    .findByIdAndShopIdAndIsActiveTrue(item.getVariantId(), shopId)
                    .orElse(null);

            if (variant != null && Boolean.TRUE.equals(variant.getTrackStock())) {
                InventoryStock stock = inventoryStockRepository
                        .findByVariantIdAndShopIdAndIsActiveTrue(item.getVariantId(), shopId)
                        .orElseThrow(() -> new IllegalArgumentException(
                                "No stock record found for variant: " + item.getSku()));

                BigDecimal newQty = stock.getCurrentQuantity().subtract(item.getQuantity());
                if (newQty.compareTo(BigDecimal.ZERO) < 0) {
                    throw new IllegalArgumentException(
                            "Insufficient stock for " + item.getProductName() +
                            " (" + item.getSku() + "). Available: " + stock.getCurrentQuantity() +
                            ", Requested: " + item.getQuantity());
                }
                stock.setCurrentQuantity(newQty);
                inventoryStockRepository.save(stock);
            }
        }
    }

    private void restoreStock(SalesOrder order, UUID shopId) {
        for (SalesOrderItem item : order.getItems()) {
            ProductVariant variant = productVariantRepository
                    .findByIdAndShopIdAndIsActiveTrue(item.getVariantId(), shopId)
                    .orElse(null);

            if (variant != null && Boolean.TRUE.equals(variant.getTrackStock())) {
                inventoryStockRepository.findByVariantIdAndShopIdAndIsActiveTrue(item.getVariantId(), shopId)
                        .ifPresent(stock -> {
                            stock.setCurrentQuantity(stock.getCurrentQuantity().add(item.getQuantity()));
                            inventoryStockRepository.save(stock);
                        });
            }
        }
    }

    // ==================== CUSTOMER ====================

    private void updateCustomerStats(SalesOrder order, UUID shopId) {
        if (order.getCustomerId() == null) return;

        customerRepository.findByIdAndShopIdAndIsActiveTrue(order.getCustomerId(), shopId)
                .ifPresent(customer -> {
                    customer.setTotalPurchases(
                            customer.getTotalPurchases().add(order.getTotalAmount()));
                    customer.setLastPurchaseDate(LocalDateTime.now());
                    customerRepository.save(customer);
                });
    }

    private void reverseCustomerStats(SalesOrder order, UUID shopId) {
        if (order.getCustomerId() == null) return;

        customerRepository.findByIdAndShopIdAndIsActiveTrue(order.getCustomerId(), shopId)
                .ifPresent(customer -> {
                    customer.setTotalPurchases(
                            customer.getTotalPurchases().subtract(order.getTotalAmount()));
                    customerRepository.save(customer);
                });
    }

    private void handleCreditPayments(SalesOrder order, UUID shopId) {
        if (order.getCustomerId() == null) return;

        BigDecimal creditTotal = order.getPayments().stream()
                .filter(p -> p.getPaymentMethod() == PaymentMethod.CREDIT)
                .map(SalesPayment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (creditTotal.compareTo(BigDecimal.ZERO) > 0) {
            Customer customer = customerRepository.findByIdAndShopIdAndIsActiveTrue(order.getCustomerId(), shopId)
                    .orElseThrow(() -> new IllegalArgumentException("Customer not found for credit payment"));

            if (customer.getBalanceCredit().compareTo(creditTotal) < 0) {
                throw new IllegalArgumentException(
                        "Insufficient customer credit. Available: " + customer.getBalanceCredit() +
                        ", Required: " + creditTotal);
            }
            customer.setBalanceCredit(customer.getBalanceCredit().subtract(creditTotal));
            customerRepository.save(customer);
        }
    }

    private void restoreCreditPayments(SalesOrder order, UUID shopId) {
        if (order.getCustomerId() == null) return;

        BigDecimal creditTotal = order.getPayments().stream()
                .filter(p -> p.getPaymentMethod() == PaymentMethod.CREDIT)
                .map(SalesPayment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (creditTotal.compareTo(BigDecimal.ZERO) > 0) {
            customerRepository.findByIdAndShopIdAndIsActiveTrue(order.getCustomerId(), shopId)
                    .ifPresent(customer -> {
                        customer.setBalanceCredit(customer.getBalanceCredit().add(creditTotal));
                        customerRepository.save(customer);
                    });
        }
    }

    // ==================== RESPONSE BUILDING ====================

    private SalesOrderResponse buildOrderResponse(SalesOrder order, UUID shopId) {
        SalesOrderResponse response = modelMapper.map(order, SalesOrderResponse.class);

        // Resolve customer name
        if (order.getCustomerId() != null) {
            customerRepository.findByIdAndShopIdAndIsActiveTrue(order.getCustomerId(), shopId)
                    .ifPresent(customer -> response.setCustomerName(customer.getCustomerName()));
        }

        // Map items
        List<SalesOrderItemResponse> itemResponses = order.getItems().stream()
                .map(item -> modelMapper.map(item, SalesOrderItemResponse.class))
                .toList();
        response.setItems(itemResponses);

        // Map payments
        List<SalesPaymentResponse> paymentResponses = order.getPayments().stream()
                .map(payment -> modelMapper.map(payment, SalesPaymentResponse.class))
                .toList();
        response.setPayments(paymentResponses);

        return response;
    }

    // ==================== HELPERS ====================

    private String generateOrderNumber(UUID shopId) {
        long count = salesOrderRepository.countByShopId(shopId);
        String code;
        do {
            count++;
            code = String.format("ORD-%04d", count);
        } while (salesOrderRepository.existsByShopIdAndOrderNumber(shopId, code));
        return code;
    }

    private UUID getCurrentShopId() {
        UserPrincipal principal = (UserPrincipal) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
        UUID shopId = principal.getShopId();
        if (shopId == null) {
            throw new IllegalArgumentException("Shop context is required");
        }
        return shopId;
    }

    private UUID getCurrentUserId() {
        UserPrincipal principal = (UserPrincipal) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
        return principal.getId();
    }
}
