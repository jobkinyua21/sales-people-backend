package com.salespeople.batchorder;

import com.salespeople.salesperson.SalesPerson;
import com.salespeople.salesperson.SalesPersonRepository;
import com.salespeople.salesorder.SalesOrderHeader;
import com.salespeople.salesorder.SalesOrderHeaderRepository;
import com.salespeople.salesorder.SalesOrderStatus;
import com.salespeople.security.SecurityContextUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SalesPersonBatchOrderService {

    private final SalesPersonBatchOrderRepository batchOrderRepository;
    private final SalesOrderHeaderRepository salesOrderHeaderRepository;
    private final SalesPersonRepository salesPersonRepository;

    @Transactional
    public List<SalesPersonBatchOrderResponse> createBatch(SalesPersonBatchOrderRequest request) {
        var principal = SecurityContextUtil.getCurrentPrincipal();
        String currentUser = principal.getUsername();
        Integer staffNumber = principal.getStaffNumber();

        // Check sales person eligibility
        SalesPerson salesPerson = salesPersonRepository.findByStaffId(staffNumber)
                .orElseThrow(() -> new IllegalArgumentException("Sales person profile not found"));

        if (Boolean.FALSE.equals(salesPerson.getMakeOrder())) {
            throw new IllegalArgumentException("You are not eligible to create a batch. Contact your manager.");
        }

        // Fetch all NEW orders for this salesperson on the given date
        List<SalesOrderHeader> newOrders = salesOrderHeaderRepository
                .findBySalesPersonNumberAndSalesOrderDateAndStatus(
                        staffNumber, request.getOrderDate(), SalesOrderStatus.NEW);

        if (newOrders.isEmpty()) {
            throw new IllegalArgumentException(
                    "No new orders found for " + request.getOrderDate() + ". Create orders first.");
        }

        // Check if any order is already in a batch
        for (SalesOrderHeader order : newOrders) {
            if (batchOrderRepository.findBySalesOrderHeaderId(order.getSalesOrderHeaderId()).isPresent()) {
                throw new IllegalArgumentException(
                        "Order #" + order.getSalesOrderNumber() + " is already in a batch");
            }
        }

        // Check total value against order limit
        BigDecimal batchTotal = newOrders.stream()
                .map(o -> o.getTotal() != null ? o.getTotal() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (batchTotal.compareTo(salesPerson.getOrderLimit()) > 0) {
            throw new IllegalArgumentException(
                    "Batch total of " + batchTotal + " exceeds your order limit of " + salesPerson.getOrderLimit());
        }

        String batchRef = "BATCH-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        // Set all orders to PENDING and create batch entries
        List<SalesPersonBatchOrder> entries = newOrders.stream().map(order -> {
            order.setStatus(SalesOrderStatus.PENDING);
            salesOrderHeaderRepository.save(order);

            return SalesPersonBatchOrder.builder()
                    .batchRef(batchRef)
                    .salesOrderHeaderId(order.getSalesOrderHeaderId())
                    .salesPersonNumber(staffNumber)
                    .orderDate(request.getOrderDate())
                    .status(SalesPersonBatchOrderStatus.PENDING)
                    .createdBy(currentUser)
                    .build();
        }).toList();

        return batchOrderRepository.saveAll(entries).stream()
                .map(e -> toResponse(e, salesPerson.getOrderLimit(), batchTotal))
                .toList();
    }

    @Transactional
    public List<SalesPersonBatchOrderResponse> approveBatch(String batchRef) {
        var principal = SecurityContextUtil.getCurrentPrincipal();
        String reviewer = principal.getUsername();

        List<SalesPersonBatchOrder> entries = batchOrderRepository.findByBatchRef(batchRef);
        if (entries.isEmpty()) {
            throw new IllegalArgumentException("Batch not found: " + batchRef);
        }

        entries.forEach(e -> {
            e.setStatus(SalesPersonBatchOrderStatus.APPROVED);
            e.setReviewedBy(reviewer);
            e.setReviewedAt(OffsetDateTime.now());

            SalesOrderHeader header = salesOrderHeaderRepository.findById(e.getSalesOrderHeaderId())
                    .orElseThrow(() -> new IllegalArgumentException("Sales order not found: " + e.getSalesOrderHeaderId()));
            header.setStatus(SalesOrderStatus.APPROVED);
            salesOrderHeaderRepository.save(header);
        });

        return batchOrderRepository.saveAll(entries).stream()
                .map(e -> toResponse(e, null, null))
                .toList();
    }

    @Transactional
    public List<SalesPersonBatchOrderResponse> rejectBatch(String batchRef, SalesPersonBatchOrderReviewRequest request) {
        var principal = SecurityContextUtil.getCurrentPrincipal();
        String reviewer = principal.getUsername();

        List<SalesPersonBatchOrder> entries = batchOrderRepository.findByBatchRef(batchRef);
        if (entries.isEmpty()) {
            throw new IllegalArgumentException("Batch not found: " + batchRef);
        }

        entries.forEach(e -> {
            e.setStatus(SalesPersonBatchOrderStatus.REJECTED);
            e.setReviewedBy(reviewer);
            e.setReviewedAt(OffsetDateTime.now());
            e.setRejectionReason(request.getRejectionReason());

            SalesOrderHeader header = salesOrderHeaderRepository.findById(e.getSalesOrderHeaderId())
                    .orElseThrow(() -> new IllegalArgumentException("Sales order not found: " + e.getSalesOrderHeaderId()));
            header.setStatus(SalesOrderStatus.REJECTED);
            salesOrderHeaderRepository.save(header);
        });

        return batchOrderRepository.saveAll(entries).stream()
                .map(e -> toResponse(e, null, null))
                .toList();
    }

    public List<SalesPersonBatchOrderResponse> fetchByBatchRef(String batchRef) {
        return batchOrderRepository.findByBatchRef(batchRef).stream()
                .map(e -> toResponse(e, null, null))
                .toList();
    }

    public List<SalesPersonBatchOrderResponse> fetchMyBatches() {
        Integer staffNumber = SecurityContextUtil.getCurrentPrincipal().getStaffNumber();
        SalesPerson salesPerson = salesPersonRepository.findByStaffId(staffNumber).orElse(null);
        BigDecimal orderLimit = salesPerson != null ? salesPerson.getOrderLimit() : null;

        return batchOrderRepository.findBySalesPersonNumber(staffNumber).stream()
                .map(e -> toResponse(e, orderLimit, null))
                .toList();
    }

    public List<SalesPersonBatchOrderResponse> fetchBySalesPerson(Integer salesPersonNumber) {
        return batchOrderRepository.findBySalesPersonNumber(salesPersonNumber).stream()
                .map(e -> toResponse(e, null, null))
                .toList();
    }

    public List<SalesPersonBatchOrderResponse> fetchByStatus(SalesPersonBatchOrderStatus status) {
        return batchOrderRepository.findByStatus(status).stream()
                .map(e -> toResponse(e, null, null))
                .toList();
    }

    private SalesPersonBatchOrderResponse toResponse(SalesPersonBatchOrder b, BigDecimal orderLimit, BigDecimal batchTotal) {
        return SalesPersonBatchOrderResponse.builder()
                .batchOrderId(b.getBatchOrderId())
                .batchRef(b.getBatchRef())
                .salesOrderHeaderId(b.getSalesOrderHeaderId())
                .salesPersonNumber(b.getSalesPersonNumber())
                .orderDate(b.getOrderDate())
                .status(b.getStatus())
                .reviewedBy(b.getReviewedBy())
                .reviewedAt(b.getReviewedAt())
                .rejectionReason(b.getRejectionReason())
                .createdBy(b.getCreatedBy())
                .createdAt(b.getCreatedAt())
                .orderLimit(orderLimit)
                .batchTotal(batchTotal)
                .build();
    }
}
