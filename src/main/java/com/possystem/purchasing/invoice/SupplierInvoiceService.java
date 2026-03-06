package com.possystem.purchasing.invoice;

import com.possystem.common.ListResponse;
import com.possystem.purchasing.enums.PurchaseOrderStatus;
import com.possystem.purchasing.enums.SupplierInvoiceStatus;
import com.possystem.purchasing.order.PurchaseOrder;
import com.possystem.purchasing.order.PurchaseOrderRepository;
import com.possystem.purchasing.payment.AddPurchasePaymentRequest;
import com.possystem.purchasing.payment.PurchasePayment;
import com.possystem.purchasing.payment.PurchasePaymentRepository;
import com.possystem.purchasing.payment.PurchasePaymentResponse;
import com.possystem.sales.PaymentStatus;
import com.possystem.security.SecurityContextUtil;
import com.possystem.supplier.PaymentTerms;
import com.possystem.supplier.Supplier;
import com.possystem.supplier.SupplierRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SupplierInvoiceService {

    private final SupplierInvoiceRepository supplierInvoiceRepository;
    private final SupplierInvoicePurchaseOrderRepository sipoRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final SupplierRepository supplierRepository;
    private final PurchasePaymentRepository purchasePaymentRepository;

    // ==================== CRUD ====================

    @Transactional
    public SupplierInvoiceResponse save(SupplierInvoiceRequest request) {
        UUID shopId = SecurityContextUtil.getCurrentShopId();

        if (request.getId() != null) {
            return updateInvoice(request, shopId);
        }
        return createInvoice(request, shopId);
    }

    public ListResponse<SupplierInvoiceResponse> fetch(SupplierInvoiceFetchRequest request) {
        UUID shopId = SecurityContextUtil.getCurrentShopId();

        if (request.getId() != null) {
            SupplierInvoice invoice = supplierInvoiceRepository.findByIdAndShopIdAndIsActiveTrue(request.getId(), shopId)
                    .orElseThrow(() -> new IllegalArgumentException("Supplier invoice not found"));
            List<SupplierInvoiceResponse> result = List.of(buildResponse(invoice, shopId));
            return ListResponse.of(result);
        }

        String search = request.getSearch();
        String invoiceStatus = request.getInvoiceStatus() != null ? request.getInvoiceStatus().name() : null;
        String paymentStatus = request.getPaymentStatus() != null ? request.getPaymentStatus().name() : null;
        UUID supplierId = request.getSupplierId();
        LocalDate dueDateFrom = request.getDueDateFrom();
        LocalDate dueDateTo = request.getDueDateTo();
        LocalDateTime dateFrom = request.getDateFrom();
        LocalDateTime dateTo = request.getDateTo();
        boolean overdueOnly = Boolean.TRUE.equals(request.getOverdueOnly());
        Integer limit = request.getLimit();

        if (limit == null) {
            List<SupplierInvoice> all = supplierInvoiceRepository.searchFilteredUnpaged(
                    shopId, search, invoiceStatus, paymentStatus, supplierId,
                    dueDateFrom, dueDateTo, dateFrom, dateTo, overdueOnly);
            List<SupplierInvoiceResponse> responses = all.stream()
                    .map(inv -> buildResponse(inv, shopId))
                    .toList();
            return ListResponse.of(responses);
        }

        PageRequest pageRequest = PageRequest.of(request.getStart(), limit);
        Page<SupplierInvoice> page = supplierInvoiceRepository.searchFiltered(
                shopId, search, invoiceStatus, paymentStatus, supplierId,
                dueDateFrom, dueDateTo, dateFrom, dateTo, overdueOnly, pageRequest);
        Page<SupplierInvoiceResponse> responsePage = page.map(inv -> buildResponse(inv, shopId));
        return ListResponse.from(responsePage);
    }

    @Transactional
    public void delete(UUID id) {
        UUID shopId = SecurityContextUtil.getCurrentShopId();
        SupplierInvoice invoice = supplierInvoiceRepository.findByIdAndShopIdAndIsActiveTrue(id, shopId)
                .orElseThrow(() -> new IllegalArgumentException("Supplier invoice not found"));

        if (invoice.getInvoiceStatus() != SupplierInvoiceStatus.DRAFT
                && invoice.getInvoiceStatus() != SupplierInvoiceStatus.CANCELLED) {
            throw new IllegalArgumentException("Only DRAFT or CANCELLED invoices can be deleted");
        }

        invoice.setIsActive(false);
        supplierInvoiceRepository.save(invoice);
    }

    @Transactional
    public void bulkDelete(List<UUID> ids) {
        UUID shopId = SecurityContextUtil.getCurrentShopId();
        List<SupplierInvoice> invoices = supplierInvoiceRepository.findAllByIdInAndShopIdAndIsActiveTrue(ids, shopId);
        for (SupplierInvoice inv : invoices) {
            if (inv.getInvoiceStatus() == SupplierInvoiceStatus.DRAFT
                    || inv.getInvoiceStatus() == SupplierInvoiceStatus.CANCELLED) {
                inv.setIsActive(false);
            }
        }
        supplierInvoiceRepository.saveAll(invoices);
    }

    // ==================== STATUS TRANSITIONS ====================

    @Transactional
    public SupplierInvoiceResponse approve(UUID id) {
        UUID shopId = SecurityContextUtil.getCurrentShopId();
        SupplierInvoice invoice = supplierInvoiceRepository.findByIdAndShopIdAndIsActiveTrue(id, shopId)
                .orElseThrow(() -> new IllegalArgumentException("Supplier invoice not found"));

        if (invoice.getInvoiceStatus() != SupplierInvoiceStatus.DRAFT) {
            throw new IllegalArgumentException("Only DRAFT invoices can be approved");
        }

        if (invoice.getPurchaseOrders().isEmpty()) {
            throw new IllegalArgumentException("Invoice must have at least one linked purchase order");
        }

        if (invoice.getTotalAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Invoice total amount must be greater than zero");
        }

        invoice.setInvoiceStatus(SupplierInvoiceStatus.APPROVED);
        invoice.setApprovedBy(SecurityContextUtil.getCurrentUserId());
        invoice.setApprovedAt(LocalDateTime.now());

        SupplierInvoice saved = supplierInvoiceRepository.save(invoice);
        return buildResponse(saved, shopId);
    }

    @Transactional
    public SupplierInvoiceResponse cancel(UUID id) {
        UUID shopId = SecurityContextUtil.getCurrentShopId();
        SupplierInvoice invoice = supplierInvoiceRepository.findByIdAndShopIdAndIsActiveTrue(id, shopId)
                .orElseThrow(() -> new IllegalArgumentException("Supplier invoice not found"));

        if (invoice.getInvoiceStatus() == SupplierInvoiceStatus.CANCELLED) {
            throw new IllegalArgumentException("Invoice is already cancelled");
        }

        if (invoice.getInvoiceStatus() == SupplierInvoiceStatus.APPROVED
                && invoice.getPaymentStatus() != PaymentStatus.UNPAID) {
            throw new IllegalArgumentException("Cannot cancel an invoice that has payments. Reverse payments first.");
        }

        invoice.setInvoiceStatus(SupplierInvoiceStatus.CANCELLED);
        invoice.setCancelledAt(LocalDateTime.now());

        SupplierInvoice saved = supplierInvoiceRepository.save(invoice);

        // Update linked PO payment statuses
        updateLinkedPOPaymentStatuses(invoice, shopId);

        return buildResponse(saved, shopId);
    }

    @Transactional
    public SupplierInvoiceResponse dispute(UUID id) {
        UUID shopId = SecurityContextUtil.getCurrentShopId();
        SupplierInvoice invoice = supplierInvoiceRepository.findByIdAndShopIdAndIsActiveTrue(id, shopId)
                .orElseThrow(() -> new IllegalArgumentException("Supplier invoice not found"));

        if (invoice.getInvoiceStatus() != SupplierInvoiceStatus.APPROVED) {
            throw new IllegalArgumentException("Only APPROVED invoices can be disputed");
        }

        invoice.setInvoiceStatus(SupplierInvoiceStatus.DISPUTED);

        SupplierInvoice saved = supplierInvoiceRepository.save(invoice);
        return buildResponse(saved, shopId);
    }

    @Transactional
    public SupplierInvoiceResponse resolve(UUID id) {
        UUID shopId = SecurityContextUtil.getCurrentShopId();
        SupplierInvoice invoice = supplierInvoiceRepository.findByIdAndShopIdAndIsActiveTrue(id, shopId)
                .orElseThrow(() -> new IllegalArgumentException("Supplier invoice not found"));

        if (invoice.getInvoiceStatus() != SupplierInvoiceStatus.DISPUTED) {
            throw new IllegalArgumentException("Only DISPUTED invoices can be resolved");
        }

        invoice.setInvoiceStatus(SupplierInvoiceStatus.APPROVED);

        SupplierInvoice saved = supplierInvoiceRepository.save(invoice);
        return buildResponse(saved, shopId);
    }

    // ==================== PAYMENTS ====================

    @Transactional
    public SupplierInvoiceResponse addPayment(AddPurchasePaymentRequest request) {
        UUID shopId = SecurityContextUtil.getCurrentShopId();
        SupplierInvoice invoice = supplierInvoiceRepository.findByIdAndShopIdAndIsActiveTrue(request.getInvoiceId(), shopId)
                .orElseThrow(() -> new IllegalArgumentException("Supplier invoice not found"));

        if (invoice.getInvoiceStatus() != SupplierInvoiceStatus.APPROVED) {
            throw new IllegalArgumentException("Can only add payments to APPROVED invoices");
        }

        if (request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Payment amount must be greater than zero");
        }

        if (invoice.getPaymentStatus() == PaymentStatus.PAID) {
            throw new IllegalArgumentException("Invoice is already fully paid");
        }

        BigDecimal balanceDue = invoice.getBalanceDue() != null ? invoice.getBalanceDue() : invoice.getTotalAmount();
        if (request.getAmount().compareTo(balanceDue) > 0) {
            throw new IllegalArgumentException(
                    "Payment amount (" + request.getAmount() + ") exceeds balance due (" + balanceDue + ")");
        }

        String voucherNumber = generateVoucherNumber(shopId);

        PurchasePayment payment = PurchasePayment.builder()
                .voucherNumber(voucherNumber)
                .shopId(shopId)
                .supplierInvoice(invoice)
                .paymentMethod(request.getPaymentMethod())
                .amount(request.getAmount())
                .referenceNumber(request.getReferenceNumber())
                .notes(request.getNotes())
                .paidBy(SecurityContextUtil.getCurrentUserId())
                .paidAt(LocalDateTime.now())
                .receiptUrl(request.getReceiptUrl())
                .build();

        invoice.getPayments().add(payment);
        recalculateInvoicePaymentStatus(invoice);

        SupplierInvoice saved = supplierInvoiceRepository.save(invoice);

        // Update linked PO payment statuses
        updateLinkedPOPaymentStatuses(invoice, shopId);

        // Update supplier balance
        Supplier supplier = supplierRepository.findByIdAndShopIdAndIsActiveTrue(invoice.getSupplierId(), shopId)
                .orElse(null);
        if (supplier != null) {
            supplier.setTotalPaid(supplier.getTotalPaid().add(request.getAmount()));
            supplier.setOutstandingBalance(supplier.getTotalPurchases().subtract(supplier.getTotalPaid()));
            supplierRepository.save(supplier);
        }

        return buildResponse(saved, shopId);
    }

    // ==================== FETCH PAYMENTS ====================

    public List<PurchasePaymentResponse> fetchPayments(UUID invoiceId) {
        UUID shopId = SecurityContextUtil.getCurrentShopId();
        SupplierInvoice invoice = supplierInvoiceRepository.findByIdAndShopIdAndIsActiveTrue(invoiceId, shopId)
                .orElseThrow(() -> new IllegalArgumentException("Supplier invoice not found"));

        return invoice.getPayments().stream()
                .map(p -> PurchasePaymentResponse.builder()
                        .id(p.getId())
                        .voucherNumber(p.getVoucherNumber())
                        .paymentMethod(p.getPaymentMethod())
                        .amount(p.getAmount())
                        .referenceNumber(p.getReferenceNumber())
                        .notes(p.getNotes())
                        .paidBy(p.getPaidBy())
                        .paidAt(p.getPaidAt())
                        .receiptUrl(p.getReceiptUrl())
                        .createdAt(p.getCreatedAt())
                        .build())
                .toList();
    }

    // ==================== CREATE / UPDATE ====================

    private SupplierInvoiceResponse createInvoice(SupplierInvoiceRequest request, UUID shopId) {
        // Validate supplier
        Supplier supplier = supplierRepository.findByIdAndShopIdAndIsActiveTrue(request.getSupplierId(), shopId)
                .orElseThrow(() -> new IllegalArgumentException("Supplier not found"));

        // Generate reference code
        long count = supplierInvoiceRepository.countByShopId(shopId);
        String referenceCode = String.format("SI-%04d", count + 1);

        // Compute due date if not provided
        LocalDate dueDate = request.getDueDate();
        PaymentTerms terms = request.getPaymentTerms() != null ? request.getPaymentTerms() : supplier.getPaymentTerms();
        if (dueDate == null && request.getInvoiceDate() != null && terms != null) {
            dueDate = computeDueDate(request.getInvoiceDate(), terms);
        }

        BigDecimal subtotal = request.getSubtotal() != null ? request.getSubtotal() : BigDecimal.ZERO;
        BigDecimal taxAmount = request.getTaxAmount() != null ? request.getTaxAmount() : BigDecimal.ZERO;

        // Validate totalAmount matches subtotal + taxAmount
        BigDecimal expectedTotal = subtotal.add(taxAmount);
        if (request.getTotalAmount().compareTo(expectedTotal) != 0) {
            throw new IllegalArgumentException(
                    "Total amount (" + request.getTotalAmount() + ") does not match subtotal + tax ("
                            + subtotal + " + " + taxAmount + " = " + expectedTotal + ")");
        }

        // Validate sum of allocated amounts doesn't exceed totalAmount
        BigDecimal totalAllocated = request.getPurchaseOrders().stream()
                .map(SupplierInvoicePurchaseOrderRequest::getAllocatedAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (totalAllocated.compareTo(request.getTotalAmount()) > 0) {
            throw new IllegalArgumentException(
                    "Sum of allocated amounts (" + totalAllocated + ") exceeds invoice total (" + request.getTotalAmount() + ")");
        }

        SupplierInvoice invoice = SupplierInvoice.builder()
                .shopId(shopId)
                .invoiceNumber(request.getInvoiceNumber())
                .referenceCode(referenceCode)
                .supplierId(request.getSupplierId())
                .invoiceDate(request.getInvoiceDate())
                .receivedDate(request.getReceivedDate())
                .dueDate(dueDate)
                .paymentTerms(terms)
                .subtotal(subtotal)
                .taxAmount(taxAmount)
                .totalAmount(request.getTotalAmount())
                .balanceDue(request.getTotalAmount())
                .notes(request.getNotes())
                .build();

        // Build PO links
        List<SupplierInvoicePurchaseOrder> poLinks = new ArrayList<>();
        for (SupplierInvoicePurchaseOrderRequest poReq : request.getPurchaseOrders()) {
            PurchaseOrder po = purchaseOrderRepository.findByIdAndShopIdAndIsActiveTrue(poReq.getPurchaseOrderId(), shopId)
                    .orElseThrow(() -> new IllegalArgumentException("Purchase order not found: " + poReq.getPurchaseOrderId()));

            if (!po.getSupplierId().equals(request.getSupplierId())) {
                throw new IllegalArgumentException("Purchase order " + po.getPoNumber() + " belongs to a different supplier");
            }

            if (po.getOrderStatus() == PurchaseOrderStatus.DRAFT || po.getOrderStatus() == PurchaseOrderStatus.CANCELLED) {
                throw new IllegalArgumentException("Purchase order " + po.getPoNumber() + " is in " + po.getOrderStatus() + " status");
            }

            // Validate allocatedAmount doesn't exceed PO's remaining unallocated amount
            BigDecimal alreadyAllocated = sipoRepository.sumAllocatedAmountByPurchaseOrderId(poReq.getPurchaseOrderId());
            BigDecimal remaining = po.getTotalAmount().subtract(alreadyAllocated);
            if (poReq.getAllocatedAmount().compareTo(remaining) > 0) {
                throw new IllegalArgumentException(
                        "Allocated amount " + poReq.getAllocatedAmount() + " exceeds remaining unallocated amount "
                                + remaining + " for PO " + po.getPoNumber()
                                + " (PO total: " + po.getTotalAmount() + ", already allocated: " + alreadyAllocated + ")");
            }

            poLinks.add(SupplierInvoicePurchaseOrder.builder()
                    .supplierInvoice(invoice)
                    .purchaseOrderId(poReq.getPurchaseOrderId())
                    .allocatedAmount(poReq.getAllocatedAmount())
                    .build());
        }
        invoice.setPurchaseOrders(poLinks);

        SupplierInvoice saved = supplierInvoiceRepository.save(invoice);
        return buildResponse(saved, shopId);
    }

    private SupplierInvoiceResponse updateInvoice(SupplierInvoiceRequest request, UUID shopId) {
        SupplierInvoice invoice = supplierInvoiceRepository.findByIdAndShopIdAndIsActiveTrue(request.getId(), shopId)
                .orElseThrow(() -> new IllegalArgumentException("Supplier invoice not found"));

        if (invoice.getInvoiceStatus() != SupplierInvoiceStatus.DRAFT) {
            throw new IllegalArgumentException("Only DRAFT invoices can be edited");
        }

        // Validate supplier
        Supplier supplier = supplierRepository.findByIdAndShopIdAndIsActiveTrue(request.getSupplierId(), shopId)
                .orElseThrow(() -> new IllegalArgumentException("Supplier not found"));

        // Compute due date if not provided
        LocalDate dueDate = request.getDueDate();
        PaymentTerms terms = request.getPaymentTerms() != null ? request.getPaymentTerms() : supplier.getPaymentTerms();
        if (dueDate == null && request.getInvoiceDate() != null && terms != null) {
            dueDate = computeDueDate(request.getInvoiceDate(), terms);
        }

        invoice.setInvoiceNumber(request.getInvoiceNumber());
        invoice.setSupplierId(request.getSupplierId());
        invoice.setInvoiceDate(request.getInvoiceDate());
        invoice.setReceivedDate(request.getReceivedDate());
        invoice.setDueDate(dueDate);
        invoice.setPaymentTerms(terms);
        BigDecimal updatedSubtotal = request.getSubtotal() != null ? request.getSubtotal() : BigDecimal.ZERO;
        BigDecimal updatedTaxAmount = request.getTaxAmount() != null ? request.getTaxAmount() : BigDecimal.ZERO;

        // Validate totalAmount matches subtotal + taxAmount
        BigDecimal expectedTotal = updatedSubtotal.add(updatedTaxAmount);
        if (request.getTotalAmount().compareTo(expectedTotal) != 0) {
            throw new IllegalArgumentException(
                    "Total amount (" + request.getTotalAmount() + ") does not match subtotal + tax ("
                            + updatedSubtotal + " + " + updatedTaxAmount + " = " + expectedTotal + ")");
        }

        // Validate sum of allocated amounts doesn't exceed totalAmount
        BigDecimal totalAllocated = request.getPurchaseOrders().stream()
                .map(SupplierInvoicePurchaseOrderRequest::getAllocatedAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (totalAllocated.compareTo(request.getTotalAmount()) > 0) {
            throw new IllegalArgumentException(
                    "Sum of allocated amounts (" + totalAllocated + ") exceeds invoice total (" + request.getTotalAmount() + ")");
        }

        invoice.setSubtotal(updatedSubtotal);
        invoice.setTaxAmount(updatedTaxAmount);
        invoice.setTotalAmount(request.getTotalAmount());
        invoice.setBalanceDue(request.getTotalAmount());
        invoice.setNotes(request.getNotes());

        // Capture previous allocations before clearing (for validation)
        List<SupplierInvoicePurchaseOrder> previousAllocations = new ArrayList<>(invoice.getPurchaseOrders());

        // Rebuild PO links
        invoice.getPurchaseOrders().clear();
        for (SupplierInvoicePurchaseOrderRequest poReq : request.getPurchaseOrders()) {
            PurchaseOrder po = purchaseOrderRepository.findByIdAndShopIdAndIsActiveTrue(poReq.getPurchaseOrderId(), shopId)
                    .orElseThrow(() -> new IllegalArgumentException("Purchase order not found: " + poReq.getPurchaseOrderId()));

            if (!po.getSupplierId().equals(request.getSupplierId())) {
                throw new IllegalArgumentException("Purchase order " + po.getPoNumber() + " belongs to a different supplier");
            }

            if (po.getOrderStatus() == PurchaseOrderStatus.DRAFT || po.getOrderStatus() == PurchaseOrderStatus.CANCELLED) {
                throw new IllegalArgumentException("Purchase order " + po.getPoNumber() + " is in " + po.getOrderStatus() + " status");
            }

            // Validate allocatedAmount doesn't exceed PO's remaining unallocated amount
            // Exclude this invoice's previous allocation (since we're rebuilding)
            BigDecimal alreadyAllocated = sipoRepository.sumAllocatedAmountByPurchaseOrderId(poReq.getPurchaseOrderId());
            BigDecimal previousAllocation = previousAllocations.stream()
                    .filter(sipo -> sipo.getPurchaseOrderId().equals(poReq.getPurchaseOrderId()))
                    .map(SupplierInvoicePurchaseOrder::getAllocatedAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal remaining = po.getTotalAmount().subtract(alreadyAllocated).add(previousAllocation);
            if (poReq.getAllocatedAmount().compareTo(remaining) > 0) {
                throw new IllegalArgumentException(
                        "Allocated amount " + poReq.getAllocatedAmount() + " exceeds remaining unallocated amount "
                                + remaining + " for PO " + po.getPoNumber()
                                + " (PO total: " + po.getTotalAmount() + ", already allocated elsewhere: "
                                + alreadyAllocated.subtract(previousAllocation) + ")");
            }

            invoice.getPurchaseOrders().add(SupplierInvoicePurchaseOrder.builder()
                    .supplierInvoice(invoice)
                    .purchaseOrderId(poReq.getPurchaseOrderId())
                    .allocatedAmount(poReq.getAllocatedAmount())
                    .build());
        }

        SupplierInvoice saved = supplierInvoiceRepository.save(invoice);
        return buildResponse(saved, shopId);
    }

    // ==================== HELPERS ====================

    private void recalculateInvoicePaymentStatus(SupplierInvoice invoice) {
        BigDecimal totalPaid = invoice.getPayments().stream()
                .map(PurchasePayment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        invoice.setAmountPaid(totalPaid);
        invoice.setBalanceDue(invoice.getTotalAmount().subtract(totalPaid));

        if (totalPaid.compareTo(BigDecimal.ZERO) == 0) {
            invoice.setPaymentStatus(PaymentStatus.UNPAID);
        } else if (totalPaid.compareTo(invoice.getTotalAmount()) >= 0) {
            invoice.setPaymentStatus(PaymentStatus.PAID);
        } else {
            invoice.setPaymentStatus(PaymentStatus.PARTIAL);
        }
    }

    private void updateLinkedPOPaymentStatuses(SupplierInvoice invoice, UUID shopId) {
        for (SupplierInvoicePurchaseOrder sipo : invoice.getPurchaseOrders()) {
            recalculatePOPaymentStatus(sipo.getPurchaseOrderId(), shopId);
        }
    }

    /**
     * Recalculates a PO's cached paymentStatus and amountPaid from all active invoices
     * that reference it. Called by SupplierInvoiceService when payments change.
     */
    public void recalculatePOPaymentStatus(UUID purchaseOrderId, UUID shopId) {
        PurchaseOrder po = purchaseOrderRepository.findByIdAndShopIdAndIsActiveTrue(purchaseOrderId, shopId)
                .orElse(null);
        if (po == null) return;

        List<SupplierInvoicePurchaseOrder> links = sipoRepository.findActiveByPurchaseOrderId(purchaseOrderId);

        if (links.isEmpty()) {
            po.setPaymentStatus(PaymentStatus.UNPAID);
            po.setAmountPaid(BigDecimal.ZERO);
            purchaseOrderRepository.save(po);
            return;
        }

        BigDecimal totalPaidForPO = BigDecimal.ZERO;

        for (SupplierInvoicePurchaseOrder link : links) {
            SupplierInvoice inv = link.getSupplierInvoice();
            if (inv.getTotalAmount().compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal ratio = inv.getAmountPaid()
                        .divide(inv.getTotalAmount(), 10, RoundingMode.HALF_UP)
                        .min(BigDecimal.ONE);
                BigDecimal paidForThisPO = link.getAllocatedAmount()
                        .multiply(ratio)
                        .setScale(2, RoundingMode.HALF_UP);
                totalPaidForPO = totalPaidForPO.add(paidForThisPO);
            }
        }

        po.setAmountPaid(totalPaidForPO);
        if (totalPaidForPO.compareTo(BigDecimal.ZERO) == 0) {
            po.setPaymentStatus(PaymentStatus.UNPAID);
        } else if (totalPaidForPO.compareTo(po.getTotalAmount()) >= 0) {
            po.setPaymentStatus(PaymentStatus.PAID);
        } else {
            po.setPaymentStatus(PaymentStatus.PARTIAL);
        }
        purchaseOrderRepository.save(po);
    }

    private String generateVoucherNumber(UUID shopId) {
        long count = purchasePaymentRepository.countByShopId(shopId);
        String code;
        do {
            count++;
            code = String.format("PV-%05d", count);
        } while (purchasePaymentRepository.existsByShopIdAndVoucherNumber(shopId, code));
        return code;
    }

    private LocalDate computeDueDate(LocalDate invoiceDate, PaymentTerms terms) {
        if (invoiceDate == null || terms == null) return null;
        return switch (terms) {
            case CASH_ON_DELIVERY, PREPAID -> invoiceDate;
            case NET_7 -> invoiceDate.plusDays(7);
            case NET_15 -> invoiceDate.plusDays(15);
            case NET_30 -> invoiceDate.plusDays(30);
            case NET_60 -> invoiceDate.plusDays(60);
            case NET_90 -> invoiceDate.plusDays(90);
            case END_OF_MONTH -> invoiceDate.withDayOfMonth(invoiceDate.lengthOfMonth());
        };
    }

    // ==================== RESPONSE BUILDER ====================

    private SupplierInvoiceResponse buildResponse(SupplierInvoice invoice, UUID shopId) {
        String supplierName = null;
        Supplier supplier = supplierRepository.findByIdAndShopIdAndIsActiveTrue(invoice.getSupplierId(), shopId).orElse(null);
        if (supplier != null) {
            supplierName = supplier.getSupplierName();
        }

        List<SupplierInvoicePurchaseOrderResponse> poResponses = invoice.getPurchaseOrders().stream()
                .map(sipo -> {
                    String poNumber = null;
                    PurchaseOrder po = purchaseOrderRepository.findByIdAndShopIdAndIsActiveTrue(sipo.getPurchaseOrderId(), shopId).orElse(null);
                    if (po != null) {
                        poNumber = po.getPoNumber();
                    }
                    return SupplierInvoicePurchaseOrderResponse.builder()
                            .purchaseOrderId(sipo.getPurchaseOrderId())
                            .poNumber(poNumber)
                            .allocatedAmount(sipo.getAllocatedAmount())
                            .build();
                })
                .toList();

        boolean isOverdue = invoice.getDueDate() != null
                && invoice.getDueDate().isBefore(LocalDate.now())
                && invoice.getPaymentStatus() != PaymentStatus.PAID;

        return SupplierInvoiceResponse.builder()
                .id(invoice.getId())
                .invoiceNumber(invoice.getInvoiceNumber())
                .referenceCode(invoice.getReferenceCode())
                .supplierId(invoice.getSupplierId())
                .supplierName(supplierName)
                .invoiceStatus(invoice.getInvoiceStatus())
                .paymentStatus(invoice.getPaymentStatus())
                .invoiceDate(invoice.getInvoiceDate())
                .receivedDate(invoice.getReceivedDate())
                .dueDate(invoice.getDueDate())
                .paymentTerms(invoice.getPaymentTerms())
                .subtotal(invoice.getSubtotal())
                .taxAmount(invoice.getTaxAmount())
                .totalAmount(invoice.getTotalAmount())
                .amountPaid(invoice.getAmountPaid())
                .balanceDue(invoice.getBalanceDue())
                .isOverdue(isOverdue)
                .notes(invoice.getNotes())
                .approvedBy(invoice.getApprovedBy())
                .approvedAt(invoice.getApprovedAt())
                .cancelledAt(invoice.getCancelledAt())
                .createdAt(invoice.getCreatedAt())
                .updatedAt(invoice.getUpdatedAt())
                .purchaseOrders(poResponses)
                .build();
    }
}
