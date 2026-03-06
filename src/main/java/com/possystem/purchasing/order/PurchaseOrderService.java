package com.possystem.purchasing.order;

import com.possystem.common.ListResponse;
import com.possystem.inventory.Product;
import com.possystem.inventory.ProductRepository;
import com.possystem.inventory.ProductVariant;
import com.possystem.inventory.ProductVariantRepository;
import com.possystem.purchasing.enums.PurchaseOrderStatus;
import com.possystem.purchasing.enums.SupplierInvoiceStatus;
import com.possystem.purchasing.invoice.*;
import com.possystem.sales.DiscountType;
import com.possystem.security.SecurityContextUtil;
import com.possystem.supplier.Supplier;
import com.possystem.supplier.SupplierRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
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
public class PurchaseOrderService {

    private final PurchaseOrderRepository purchaseOrderRepository;
    private final ProductVariantRepository productVariantRepository;
    private final ProductRepository productRepository;
    private final SupplierRepository supplierRepository;
    private final SupplierInvoicePurchaseOrderRepository sipoRepository;

    // ==================== CRUD ====================

    @Transactional
    public PurchaseOrderResponse save(PurchaseOrderRequest request) {
        UUID shopId = SecurityContextUtil.getCurrentShopId();

        if (request.getId() != null) {
            return updateOrder(request, shopId);
        }
        return createOrder(request, shopId);
    }

    public ListResponse<PurchaseOrderResponse> fetch(PurchaseOrderFetchRequest request) {
        UUID shopId = SecurityContextUtil.getCurrentShopId();

        if (request.getId() != null) {
            PurchaseOrder po = purchaseOrderRepository.findByIdAndShopIdAndIsActiveTrue(request.getId(), shopId)
                    .orElseThrow(() -> new IllegalArgumentException("Purchase order not found"));
            List<PurchaseOrderResponse> result = List.of(buildResponse(po, shopId));
            return ListResponse.of(result);
        }

        String search = request.getSearch();
        String orderStatus = request.getOrderStatus() != null ? request.getOrderStatus().name() : null;
        String paymentStatus = request.getPaymentStatus() != null ? request.getPaymentStatus().name() : null;
        UUID supplierId = request.getSupplierId();
        LocalDateTime dateFrom = request.getDateFrom();
        LocalDateTime dateTo = request.getDateTo();
        Integer limit = request.getLimit();

        if (limit == null) {
            List<PurchaseOrder> all = purchaseOrderRepository.searchFilteredUnpaged(
                    shopId, search, orderStatus, paymentStatus, supplierId, dateFrom, dateTo);
            List<PurchaseOrderResponse> responses = all.stream()
                    .map(po -> buildResponse(po, shopId))
                    .toList();
            return ListResponse.of(responses);
        }

        PageRequest pageRequest = PageRequest.of(request.getStart(), limit);
        Page<PurchaseOrder> page = purchaseOrderRepository.searchFiltered(
                shopId, search, orderStatus, paymentStatus, supplierId, dateFrom, dateTo, pageRequest);
        Page<PurchaseOrderResponse> responsePage = page.map(po -> buildResponse(po, shopId));
        return ListResponse.from(responsePage);
    }

    @Transactional
    public void delete(UUID id) {
        UUID shopId = SecurityContextUtil.getCurrentShopId();
        PurchaseOrder po = purchaseOrderRepository.findByIdAndShopIdAndIsActiveTrue(id, shopId)
                .orElseThrow(() -> new IllegalArgumentException("Purchase order not found"));

        if (po.getOrderStatus() != PurchaseOrderStatus.DRAFT && po.getOrderStatus() != PurchaseOrderStatus.CANCELLED) {
            throw new IllegalArgumentException("Only DRAFT or CANCELLED purchase orders can be deleted");
        }

        po.setIsActive(false);
        purchaseOrderRepository.save(po);
    }

    @Transactional
    public void bulkDelete(List<UUID> ids) {
        UUID shopId = SecurityContextUtil.getCurrentShopId();
        List<PurchaseOrder> orders = purchaseOrderRepository.findAllByIdInAndShopIdAndIsActiveTrue(ids, shopId);
        for (PurchaseOrder po : orders) {
            if (po.getOrderStatus() == PurchaseOrderStatus.DRAFT || po.getOrderStatus() == PurchaseOrderStatus.CANCELLED) {
                po.setIsActive(false);
            }
        }
        purchaseOrderRepository.saveAll(orders);
    }

    // ==================== STATUS TRANSITIONS ====================

    @Transactional
    public PurchaseOrderResponse submitOrder(UUID id) {
        UUID shopId = SecurityContextUtil.getCurrentShopId();
        PurchaseOrder po = purchaseOrderRepository.findByIdAndShopIdAndIsActiveTrue(id, shopId)
                .orElseThrow(() -> new IllegalArgumentException("Purchase order not found"));

        if (po.getOrderStatus() != PurchaseOrderStatus.DRAFT) {
            throw new IllegalArgumentException("Only DRAFT orders can be submitted");
        }

        po.setOrderStatus(PurchaseOrderStatus.ORDERED);
        po.setOrderedBy(SecurityContextUtil.getCurrentUserId());
        po.setOrderedAt(LocalDateTime.now());
        if (po.getOrderDate() == null) {
            po.setOrderDate(java.time.LocalDate.now());
        }

        PurchaseOrder saved = purchaseOrderRepository.save(po);

        // Update supplier balance
        Supplier supplier = supplierRepository.findByIdAndShopIdAndIsActiveTrue(po.getSupplierId(), shopId)
                .orElse(null);
        if (supplier != null) {
            supplier.setTotalPurchases(supplier.getTotalPurchases().add(po.getTotalAmount()));
            supplier.setOutstandingBalance(supplier.getTotalPurchases().subtract(supplier.getTotalPaid()));
            supplierRepository.save(supplier);
        }

        return buildResponse(saved, shopId);
    }

    @Transactional
    public PurchaseOrderResponse cancelOrder(UUID id) {
        UUID shopId = SecurityContextUtil.getCurrentShopId();
        PurchaseOrder po = purchaseOrderRepository.findByIdAndShopIdAndIsActiveTrue(id, shopId)
                .orElseThrow(() -> new IllegalArgumentException("Purchase order not found"));

        if (po.getOrderStatus() == PurchaseOrderStatus.RECEIVED || po.getOrderStatus() == PurchaseOrderStatus.CANCELLED) {
            throw new IllegalArgumentException("Cannot cancel a fully received or already cancelled order");
        }

        // Reverse supplier balance if order was submitted (not DRAFT)
        if (po.getOrderStatus() != PurchaseOrderStatus.DRAFT) {
            Supplier supplier = supplierRepository.findByIdAndShopIdAndIsActiveTrue(po.getSupplierId(), shopId)
                    .orElse(null);
            if (supplier != null) {
                supplier.setTotalPurchases(supplier.getTotalPurchases().subtract(po.getTotalAmount()));
                supplier.setOutstandingBalance(supplier.getTotalPurchases().subtract(supplier.getTotalPaid()));
                supplierRepository.save(supplier);
            }
        }

        po.setOrderStatus(PurchaseOrderStatus.CANCELLED);
        po.setCancelledAt(LocalDateTime.now());

        PurchaseOrder saved = purchaseOrderRepository.save(po);
        return buildResponse(saved, shopId);
    }

    // ==================== INTERNAL — called by GRN service ====================

    @Transactional
    public void updateReceivingStatus(UUID purchaseOrderId, UUID shopId) {
        PurchaseOrder po = purchaseOrderRepository.findByIdAndShopIdAndIsActiveTrue(purchaseOrderId, shopId)
                .orElseThrow(() -> new IllegalArgumentException("Purchase order not found"));

        boolean allFullyReceived = true;
        boolean anyReceived = false;

        for (PurchaseOrderItem item : po.getItems()) {
            BigDecimal totalAccounted = item.getReceivedQuantity().add(item.getDamagedQuantity());
            if (totalAccounted.compareTo(BigDecimal.ZERO) > 0) {
                anyReceived = true;
            }
            if (totalAccounted.compareTo(item.getQuantity()) < 0) {
                allFullyReceived = false;
            }
        }

        if (allFullyReceived && anyReceived) {
            po.setOrderStatus(PurchaseOrderStatus.RECEIVED);
            po.setCompletedAt(LocalDateTime.now());
        } else if (anyReceived) {
            po.setOrderStatus(PurchaseOrderStatus.PARTIALLY_RECEIVED);
        }

        purchaseOrderRepository.save(po);
    }

    // ==================== CREATE / UPDATE ====================

    private PurchaseOrderResponse createOrder(PurchaseOrderRequest request, UUID shopId) {
        // Validate supplier
        supplierRepository.findByIdAndShopIdAndIsActiveTrue(request.getSupplierId(), shopId)
                .orElseThrow(() -> new IllegalArgumentException("Supplier not found"));

        // Generate PO number
        long count = purchaseOrderRepository.countByShopId(shopId);
        String poNumber = String.format("PO-%04d", count + 1);

        PurchaseOrder po = PurchaseOrder.builder()
                .shopId(shopId)
                .poNumber(poNumber)
                .supplierId(request.getSupplierId())
                .orderDate(request.getOrderDate())
                .expectedDate(request.getExpectedDate())
                .taxRate(request.getTaxRate() != null ? request.getTaxRate() : BigDecimal.ZERO)
                .discountType(request.getDiscountType())
                .discountValue(request.getDiscountValue())
                .referenceNumber(request.getReferenceNumber())
                .notes(request.getNotes())
                .build();

        // Build items
        List<PurchaseOrderItem> items = new ArrayList<>();
        for (PurchaseOrderItemRequest itemReq : request.getItems()) {
            items.add(buildOrderItem(itemReq, po, shopId));
        }
        po.setItems(items);

        // Compute totals
        computeOrderTotals(po);

        PurchaseOrder saved = purchaseOrderRepository.save(po);
        return buildResponse(saved, shopId);
    }

    private PurchaseOrderResponse updateOrder(PurchaseOrderRequest request, UUID shopId) {
        PurchaseOrder po = purchaseOrderRepository.findByIdAndShopIdAndIsActiveTrue(request.getId(), shopId)
                .orElseThrow(() -> new IllegalArgumentException("Purchase order not found"));

        if (po.getOrderStatus() != PurchaseOrderStatus.DRAFT) {
            throw new IllegalArgumentException("Only DRAFT orders can be updated");
        }

        // Validate supplier
        supplierRepository.findByIdAndShopIdAndIsActiveTrue(request.getSupplierId(), shopId)
                .orElseThrow(() -> new IllegalArgumentException("Supplier not found"));

        po.setSupplierId(request.getSupplierId());
        po.setOrderDate(request.getOrderDate());
        po.setExpectedDate(request.getExpectedDate());
        po.setTaxRate(request.getTaxRate() != null ? request.getTaxRate() : BigDecimal.ZERO);
        po.setDiscountType(request.getDiscountType());
        po.setDiscountValue(request.getDiscountValue());
        po.setReferenceNumber(request.getReferenceNumber());
        po.setNotes(request.getNotes());

        // Rebuild items
        po.getItems().clear();
        for (PurchaseOrderItemRequest itemReq : request.getItems()) {
            po.getItems().add(buildOrderItem(itemReq, po, shopId));
        }

        // Recompute totals
        computeOrderTotals(po);

        PurchaseOrder saved = purchaseOrderRepository.save(po);
        return buildResponse(saved, shopId);
    }

    private PurchaseOrderItem buildOrderItem(PurchaseOrderItemRequest itemReq, PurchaseOrder po, UUID shopId) {
        ProductVariant variant = productVariantRepository.findByIdAndShopIdAndIsActiveTrue(itemReq.getVariantId(), shopId)
                .orElseThrow(() -> new IllegalArgumentException("Product variant not found: " + itemReq.getVariantId()));

        Product product = productRepository.findByIdAndShopIdAndIsActiveTrue(variant.getProductId(), shopId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found for variant: " + variant.getVariantName()));

        // Compute item discount
        BigDecimal lineTotal = itemReq.getUnitCost().multiply(itemReq.getQuantity());
        BigDecimal itemDiscount = computeDiscount(lineTotal, itemReq.getDiscountType(), itemReq.getDiscountValue());
        BigDecimal totalCost = lineTotal.subtract(itemDiscount);

        return PurchaseOrderItem.builder()
                .purchaseOrder(po)
                .variantId(variant.getId())
                .productName(product.getProductName())
                .variantName(variant.getVariantName())
                .sku(variant.getSku())
                .quantity(itemReq.getQuantity())
                .unitCost(itemReq.getUnitCost())
                .discountType(itemReq.getDiscountType())
                .discountValue(itemReq.getDiscountValue())
                .discountAmount(itemDiscount)
                .totalCost(totalCost)
                .build();
    }

    // ==================== COMPUTATIONS ====================

    private void computeOrderTotals(PurchaseOrder po) {
        BigDecimal subtotal = po.getItems().stream()
                .map(PurchaseOrderItem::getTotalCost)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        po.setSubtotal(subtotal);

        // Order-level discount
        BigDecimal orderDiscount = computeDiscount(subtotal, po.getDiscountType(), po.getDiscountValue());
        po.setDiscountAmount(orderDiscount);

        BigDecimal afterDiscount = subtotal.subtract(orderDiscount);

        // Tax
        BigDecimal taxRate = po.getTaxRate() != null ? po.getTaxRate() : BigDecimal.ZERO;
        BigDecimal taxAmount = afterDiscount.multiply(taxRate)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        po.setTaxAmount(taxAmount);

        // Distribute tax to items proportionally
        if (taxAmount.compareTo(BigDecimal.ZERO) > 0 && afterDiscount.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal distributedTax = BigDecimal.ZERO;
            List<PurchaseOrderItem> items = po.getItems();
            for (int i = 0; i < items.size(); i++) {
                PurchaseOrderItem item = items.get(i);
                if (i == items.size() - 1) {
                    item.setTaxAmount(taxAmount.subtract(distributedTax));
                } else {
                    BigDecimal itemTax = item.getTotalCost()
                            .divide(afterDiscount, 10, RoundingMode.HALF_UP)
                            .multiply(taxAmount)
                            .setScale(2, RoundingMode.HALF_UP);
                    item.setTaxAmount(itemTax);
                    distributedTax = distributedTax.add(itemTax);
                }
            }
        }

        BigDecimal totalAmount = afterDiscount.add(taxAmount);
        po.setTotalAmount(totalAmount);
    }

    private BigDecimal computeDiscount(BigDecimal amount, DiscountType type, BigDecimal value) {
        if (type == null || value == null || value.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        if (type == DiscountType.PERCENTAGE) {
            return amount.multiply(value)
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        }
        return value.min(amount);
    }

    // ==================== RESPONSE BUILDER ====================

    private PurchaseOrderResponse buildResponse(PurchaseOrder po, UUID shopId) {
        String supplierName = null;
        Supplier supplier = supplierRepository.findByIdAndShopIdAndIsActiveTrue(po.getSupplierId(), shopId).orElse(null);
        if (supplier != null) {
            supplierName = supplier.getSupplierName();
        }

        List<PurchaseOrderItemResponse> itemResponses = po.getItems().stream()
                .map(item -> PurchaseOrderItemResponse.builder()
                        .id(item.getId())
                        .variantId(item.getVariantId())
                        .productName(item.getProductName())
                        .variantName(item.getVariantName())
                        .sku(item.getSku())
                        .quantity(item.getQuantity())
                        .receivedQuantity(item.getReceivedQuantity())
                        .damagedQuantity(item.getDamagedQuantity())
                        .unitCost(item.getUnitCost())
                        .discountType(item.getDiscountType())
                        .discountValue(item.getDiscountValue())
                        .discountAmount(item.getDiscountAmount())
                        .taxAmount(item.getTaxAmount())
                        .totalCost(item.getTotalCost())
                        .build())
                .toList();

        // Build invoice summaries
        List<SupplierInvoicePurchaseOrder> invoiceLinks = sipoRepository.findActiveByPurchaseOrderId(po.getId());
        List<SupplierInvoiceSummaryResponse> invoiceSummaries = invoiceLinks.stream()
                .map(link -> {
                    SupplierInvoice inv = link.getSupplierInvoice();
                    return SupplierInvoiceSummaryResponse.builder()
                            .id(inv.getId())
                            .invoiceNumber(inv.getInvoiceNumber())
                            .invoiceStatus(inv.getInvoiceStatus())
                            .paymentStatus(inv.getPaymentStatus())
                            .totalAmount(inv.getTotalAmount())
                            .amountPaid(inv.getAmountPaid())
                            .build();
                })
                .toList();

        return PurchaseOrderResponse.builder()
                .id(po.getId())
                .poNumber(po.getPoNumber())
                .supplierId(po.getSupplierId())
                .supplierName(supplierName)
                .orderStatus(po.getOrderStatus())
                .paymentStatus(po.getPaymentStatus())
                .orderDate(po.getOrderDate())
                .expectedDate(po.getExpectedDate())
                .subtotal(po.getSubtotal())
                .taxRate(po.getTaxRate())
                .taxAmount(po.getTaxAmount())
                .discountType(po.getDiscountType())
                .discountValue(po.getDiscountValue())
                .discountAmount(po.getDiscountAmount())
                .totalAmount(po.getTotalAmount())
                .amountPaid(po.getAmountPaid())
                .referenceNumber(po.getReferenceNumber())
                .notes(po.getNotes())
                .orderedBy(po.getOrderedBy())
                .orderedAt(po.getOrderedAt())
                .completedAt(po.getCompletedAt())
                .cancelledAt(po.getCancelledAt())
                .createdAt(po.getCreatedAt())
                .updatedAt(po.getUpdatedAt())
                .items(itemResponses)
                .invoices(invoiceSummaries)
                .build();
    }

}
