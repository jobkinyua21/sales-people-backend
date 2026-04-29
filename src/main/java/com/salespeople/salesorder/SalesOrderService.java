package com.salespeople.salesorder;

import com.salespeople.common.ListResponse;
import com.salespeople.customer.Customer;
import com.salespeople.customer.CustomerRepository;
import com.salespeople.discount.DiscountSetup;
import com.salespeople.discount.DiscountSetupRepository;
import com.salespeople.item.ItemsRegister;
import com.salespeople.item.ItemsRegisterRepository;
import com.salespeople.security.SecurityContextUtil;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SalesOrderService {

    private final SalesOrderHeaderRepository headerRepository;
    private final SalesOrderLineRepository lineRepository;
    private final CustomerRepository customerRepository;
    private final ItemsRegisterRepository itemsRegisterRepository;
    private final DiscountSetupRepository discountSetupRepository;
    private final EntityManager entityManager;

    @Transactional
    public SalesOrderResponse createOrder(SalesOrderRequest request) {
        if (request.getSalesOrderHeaderId() != null) {
            return updateOrder(request);
        }

        var principal = SecurityContextUtil.getCurrentPrincipal();
        String currentUser = principal.getUsername();
        Integer salesPersonNumber = principal.getStaffNumber();

        Customer customer = customerRepository.findById(request.getCustomerId())
                .orElseThrow(() -> new IllegalArgumentException("Customer not found"));

        if (Boolean.TRUE.equals(customer.getDeleted())) {
            throw new IllegalArgumentException("Customer not found");
        }

        // Build header — sales_order_number is DB-sequence-generated
        SalesOrderHeader header = SalesOrderHeader.builder()
                .saleOrderType(request.getSaleOrderType())
                .salesOrderDate(request.getSalesOrderDate() != null ? request.getSalesOrderDate() : LocalDate.now())
                .customerName(customer.getCustomerOutletName())
                .phoneNumber(customer.getCustomerContact())
                .customerId(String.valueOf(customer.getCustomerId()))
                .salesPersonNumber(salesPersonNumber)
                .createdBy(currentUser)
                .status(SalesOrderStatus.NEW)
                .discount(BigDecimal.ZERO)
                .build();

        SalesOrderHeader savedHeader = headerRepository.saveAndFlush(header);

        // Force DB refresh to pick up the sequence-generated sales_order_number
        entityManager.refresh(savedHeader);

        savedHeader.setBatchNumber("BATCH-" + savedHeader.getSalesOrderNumber());
        headerRepository.saveAndFlush(savedHeader);

        List<SalesOrderLine> lines = buildLines(request.getLines(), savedHeader, currentUser);
        lineRepository.saveAll(lines);

        // Compute totals
        BigDecimal orderTotal = lines.stream()
                .map(l -> l.getTotal() != null ? l.getTotal() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalDiscount = lines.stream()
                .map(l -> l.getDiscountValue() != null ? l.getDiscountValue() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        savedHeader.setSalesOrderTotalValue(orderTotal);
        savedHeader.setTotal(orderTotal);
        savedHeader.setDiscount(totalDiscount);
        savedHeader.setNumberOfItems(lines.size());
        headerRepository.save(savedHeader);

        return toResponse(savedHeader, lines);
    }

    @Transactional
    public SalesOrderResponse updateOrder(SalesOrderRequest request) {
        var principal = SecurityContextUtil.getCurrentPrincipal();
        String currentUser = principal.getUsername();

        SalesOrderHeader header = headerRepository.findById(request.getSalesOrderHeaderId())
                .orElseThrow(() -> new IllegalArgumentException("Sales order not found"));

        if (header.getStatus() != SalesOrderStatus.NEW) {
            throw new IllegalArgumentException("Only NEW orders can be edited");
        }

        Customer customer = customerRepository.findById(request.getCustomerId())
                .orElseThrow(() -> new IllegalArgumentException("Customer not found"));

        if (Boolean.TRUE.equals(customer.getDeleted())) {
            throw new IllegalArgumentException("Customer not found");
        }

        // Update header fields
        header.setSaleOrderType(request.getSaleOrderType());
        header.setSalesOrderDate(request.getSalesOrderDate());
        header.setCustomerName(customer.getCustomerOutletName());
        header.setPhoneNumber(customer.getCustomerContact());
        header.setCustomerId(String.valueOf(customer.getCustomerId()));

        // Replace all lines
        lineRepository.deleteBySalesOrderNumber(header.getSalesOrderNumber());

        List<SalesOrderLine> lines = buildLines(request.getLines(), header, currentUser);
        lineRepository.saveAll(lines);

        // Recompute totals
        BigDecimal orderTotal = lines.stream()
                .map(l -> l.getTotal() != null ? l.getTotal() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalDiscount = lines.stream()
                .map(l -> l.getDiscountValue() != null ? l.getDiscountValue() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        header.setSalesOrderTotalValue(orderTotal);
        header.setTotal(orderTotal);
        header.setDiscount(totalDiscount);
        header.setNumberOfItems(lines.size());
        headerRepository.save(header);

        return toResponse(header, lines);
    }

    private List<SalesOrderLine> buildLines(List<SalesOrderLineRequest> lineRequests,
                                             SalesOrderHeader header, String currentUser) {
        List<SalesOrderLine> lines = new ArrayList<>();

        for (SalesOrderLineRequest req : lineRequests) {
            ItemsRegister item = itemsRegisterRepository.findByItemCode(req.getItemCode())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Item not found: " + req.getItemCode()));

            if (Boolean.TRUE.equals(item.getDisabled())) {
                throw new IllegalArgumentException("Item is disabled: " + item.getItemName());
            }

            BigDecimal unitPrice = item.getCurrentPrice();
            BigDecimal quantity = BigDecimal.valueOf(req.getQuantity());
            BigDecimal subTotal = unitPrice.multiply(quantity);

            // Apply discount if configured for this item
            BigDecimal discountValue = BigDecimal.ZERO;
            BigDecimal discountStartValue = BigDecimal.ZERO;
            Optional<DiscountSetup> discountOpt = discountSetupRepository.findByItemCode(req.getItemCode());
            if (discountOpt.isPresent()) {
                DiscountSetup ds = discountOpt.get();
                discountStartValue = ds.getDiscountStartValue() != null ? ds.getDiscountStartValue() : BigDecimal.ZERO;
                if (ds.getDiscountValue() != null && subTotal.compareTo(discountStartValue) >= 0) {
                    discountValue = ds.getDiscountValue();
                }
            }

            BigDecimal total = subTotal.subtract(discountValue);

            SalesOrderLine line = SalesOrderLine.builder()
                    .salesOrderNumber(header.getSalesOrderNumber())
                    .itemCode(item.getItemCode())
                    .itemName(item.getItemName())
                    .accountNumber(item.getAccountNumber())
                    .storeCode(req.getStoreCode())
                    .quantity(req.getQuantity())
                    .costPerItem(unitPrice)
                    .subTotal(subTotal)
                    .discountStartValue(discountStartValue)
                    .discountValue(discountValue)
                    .total(total)
                    .salesPersonNumber(header.getSalesPersonNumber())
                    .createdBy(currentUser)
                    .status("New")
                    .build();

            lines.add(line);
        }

        return lines;
    }

    public ListResponse<SalesOrderResponse> fetch(SalesOrderFetchRequest request) {
        var principal = SecurityContextUtil.getCurrentPrincipal();
        boolean isAdmin = principal.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        // Sales person always sees only their own orders; admin can filter by any
        Integer salesPersonNumber = isAdmin
                ? request.getSalesPersonNumber()
                : principal.getStaffNumber();

        if (request.getSalesOrderHeaderId() != null) {
            SalesOrderHeader header = headerRepository.findById(request.getSalesOrderHeaderId())
                    .orElseThrow(() -> new IllegalArgumentException("Sales order not found"));
            List<SalesOrderLine> lines = lineRepository.findBySalesOrderNumber(header.getSalesOrderNumber());
            return ListResponse.of(List.of(toResponse(header, lines)));
        }

        if (request.getSalesOrderNumber() != null) {
            SalesOrderHeader header = headerRepository.findBySalesOrderNumber(request.getSalesOrderNumber())
                    .orElseThrow(() -> new IllegalArgumentException("Sales order not found"));
            List<SalesOrderLine> lines = lineRepository.findBySalesOrderNumber(header.getSalesOrderNumber());
            return ListResponse.of(List.of(toResponse(header, lines)));
        }

        String statusFilter = request.getStatus() != null ? request.getStatus() : null;

        if (request.getLimit() == null) {
            List<SalesOrderHeader> all = headerRepository.searchAll(
                    salesPersonNumber, statusFilter, request.getSearch());
            return ListResponse.of(all.stream()
                    .map(h -> toResponse(h, lineRepository.findBySalesOrderNumber(h.getSalesOrderNumber())))
                    .toList());
        }

        PageRequest pageRequest = PageRequest.of(request.getStart(), request.getLimit());
        Page<SalesOrderHeader> page = headerRepository.searchAll(
                salesPersonNumber, statusFilter, request.getSearch(), pageRequest);
        return ListResponse.from(page.map(
                h -> toResponse(h, lineRepository.findBySalesOrderNumber(h.getSalesOrderNumber()))));
    }

    @Transactional
    public SalesOrderResponse cancelOrder(Long salesOrderHeaderId) {
        SalesOrderHeader header = headerRepository.findById(salesOrderHeaderId)
                .orElseThrow(() -> new IllegalArgumentException("Sales order not found"));

        if (header.getStatus() == SalesOrderStatus.POSTED) {
            throw new IllegalArgumentException("Posted orders cannot be cancelled");
        }
        if (header.getStatus() == SalesOrderStatus.CANCELLED) {
            throw new IllegalArgumentException("Order is already cancelled");
        }
        if (header.getStatus() == SalesOrderStatus.PENDING || header.getStatus() == SalesOrderStatus.APPROVED) {
            throw new IllegalArgumentException("Cannot cancel an order that is already in a batch");
        }

        header.setStatus(SalesOrderStatus.CANCELLED);
        headerRepository.save(header);

        List<SalesOrderLine> lines = lineRepository.findBySalesOrderNumber(header.getSalesOrderNumber());
        lines.forEach(l -> l.setStatus("Cancelled"));
        lineRepository.saveAll(lines);

        return toResponse(header, lines);
    }

    public List<LocalDate> getUnbatchedOrderDates() {
        Integer staffNumber = SecurityContextUtil.getCurrentPrincipal().getStaffNumber();
        return headerRepository.findUnbatchedNewOrderDates(staffNumber).stream()
                .map(java.sql.Date::toLocalDate)
                .toList();
    }

    // ==================== MAPPERS ====================

    private SalesOrderResponse toResponse(SalesOrderHeader h, List<SalesOrderLine> lines) {
        return SalesOrderResponse.builder()
                .salesOrderHeaderId(h.getSalesOrderHeaderId())
                .salesOrderNumber(h.getSalesOrderNumber())
                .saleOrderType(h.getSaleOrderType())
                .salesPersonNumber(h.getSalesPersonNumber())
                .status(h.getStatus())
                .salesOrderDate(h.getSalesOrderDate())
                .entryDate(h.getEntryDate())
                .customerName(h.getCustomerName())
                .phoneNumber(h.getPhoneNumber())
                .customerId(h.getCustomerId())
                .numberOfItems(h.getNumberOfItems())
                .salesOrderTotalValue(h.getSalesOrderTotalValue())
                .discount(h.getDiscount())
                .total(h.getTotal())
                .batchNumber(h.getBatchNumber())
                .createdBy(h.getCreatedBy())
                .lines(lines.stream().map(this::toLineResponse).toList())
                .build();
    }

    private SalesOrderLineResponse toLineResponse(SalesOrderLine l) {
        return SalesOrderLineResponse.builder()
                .orderLineId(l.getOrderLineId())
                .itemCode(l.getItemCode())
                .itemName(l.getItemName())
                .accountNumber(l.getAccountNumber())
                .accountName(l.getAccountName())
                .storeCode(l.getStoreCode())
                .storeName(l.getStoreName())
                .quantity(l.getQuantity())
                .costPerItem(l.getCostPerItem())
                .discountStartValue(l.getDiscountStartValue())
                .discountValue(l.getDiscountValue())
                .subTotal(l.getSubTotal())
                .total(l.getTotal())
                .status(l.getStatus())
                .build();
    }
}
