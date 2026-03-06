package com.possystem.purchasing.returns;

import com.possystem.common.ListResponse;
import com.possystem.inventory.InventoryStock;
import com.possystem.inventory.InventoryStockRepository;
import com.possystem.purchasing.enums.GrnStatus;
import com.possystem.purchasing.enums.PurchaseReturnStatus;
import com.possystem.purchasing.grn.GoodsReceivedNote;
import com.possystem.purchasing.grn.GoodsReceivedNoteItem;
import com.possystem.purchasing.grn.GoodsReceivedNoteRepository;
import com.possystem.purchasing.order.PurchaseOrder;
import com.possystem.purchasing.order.PurchaseOrderRepository;
import com.possystem.security.SecurityContextUtil;
import com.possystem.supplier.Supplier;
import com.possystem.supplier.SupplierRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PurchaseReturnService {

    private final PurchaseReturnRepository purchaseReturnRepository;
    private final GoodsReceivedNoteRepository grnRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final InventoryStockRepository inventoryStockRepository;
    private final SupplierRepository supplierRepository;

    // ==================== CRUD ====================

    @Transactional
    public PurchaseReturnResponse save(PurchaseReturnRequest request) {
        UUID shopId = SecurityContextUtil.getCurrentShopId();

        if (request.getId() != null) {
            return updateReturn(request, shopId);
        }
        return createReturn(request, shopId);
    }

    public ListResponse<PurchaseReturnResponse> fetch(PurchaseReturnFetchRequest request) {
        UUID shopId = SecurityContextUtil.getCurrentShopId();

        if (request.getId() != null) {
            PurchaseReturn pr = purchaseReturnRepository.findByIdAndShopIdAndIsActiveTrue(request.getId(), shopId)
                    .orElseThrow(() -> new IllegalArgumentException("Purchase return not found"));
            List<PurchaseReturnResponse> result = List.of(buildResponse(pr, shopId, true));
            return ListResponse.of(result);
        }

        String search = request.getSearch();
        String returnStatus = request.getReturnStatus() != null ? request.getReturnStatus().name() : null;
        UUID supplierId = request.getSupplierId();
        UUID grnId = request.getGrnId();
        UUID purchaseOrderId = request.getPurchaseOrderId();
        LocalDateTime dateFrom = request.getDateFrom();
        LocalDateTime dateTo = request.getDateTo();
        Integer limit = request.getLimit();

        if (limit == null) {
            List<PurchaseReturn> all = purchaseReturnRepository.searchFilteredUnpaged(
                    shopId, search, returnStatus, supplierId, grnId, purchaseOrderId, dateFrom, dateTo);
            List<PurchaseReturnResponse> responses = all.stream()
                    .map(pr -> buildResponse(pr, shopId))
                    .toList();
            return ListResponse.of(responses);
        }

        PageRequest pageRequest = PageRequest.of(request.getStart(), limit);
        Page<PurchaseReturn> page = purchaseReturnRepository.searchFiltered(
                shopId, search, returnStatus, supplierId, grnId, purchaseOrderId, dateFrom, dateTo, pageRequest);
        Page<PurchaseReturnResponse> responsePage = page.map(pr -> buildResponse(pr, shopId));
        return ListResponse.from(responsePage);
    }

    public List<ReturnableItemResponse> getReturnableItems(UUID grnId) {
        UUID shopId = SecurityContextUtil.getCurrentShopId();
        GoodsReceivedNote grn = grnRepository.findByIdAndShopIdAndIsActiveTrue(grnId, shopId)
                .orElseThrow(() -> new IllegalArgumentException("GRN not found"));

        if (grn.getGrnStatus() != GrnStatus.COMPLETED) {
            throw new IllegalArgumentException("Can only return items from COMPLETED GRNs");
        }

        return grn.getItems().stream()
                .map(item -> {
                    BigDecimal received = item.getQuantityReceived();
                    BigDecimal alreadyReturned = item.getReturnedQuantity() != null ? item.getReturnedQuantity() : BigDecimal.ZERO;
                    BigDecimal returnable = received.subtract(alreadyReturned);

                    return ReturnableItemResponse.builder()
                            .grnItemId(item.getId())
                            .variantId(item.getVariantId())
                            .productName(item.getProductName())
                            .variantName(item.getVariantName())
                            .sku(item.getSku())
                            .quantityReceived(received)
                            .quantityAlreadyReturned(alreadyReturned)
                            .quantityReturnable(returnable)
                            .unitCost(item.getUnitCost())
                            .build();
                })
                .filter(item -> item.getQuantityReturnable().compareTo(BigDecimal.ZERO) > 0)
                .toList();
    }

    // ==================== STATUS TRANSITIONS ====================

    @Transactional
    public PurchaseReturnResponse submitReturn(UUID returnId) {
        UUID shopId = SecurityContextUtil.getCurrentShopId();
        PurchaseReturn pr = purchaseReturnRepository.findByIdAndShopIdAndIsActiveTrue(returnId, shopId)
                .orElseThrow(() -> new IllegalArgumentException("Purchase return not found"));

        if (pr.getReturnStatus() != PurchaseReturnStatus.DRAFT) {
            throw new IllegalArgumentException("Only DRAFT returns can be submitted");
        }

        pr.setReturnStatus(PurchaseReturnStatus.SUBMITTED);
        pr.setSubmittedAt(LocalDateTime.now());
        pr.setReturnedBy(SecurityContextUtil.getCurrentUserId());

        PurchaseReturn saved = purchaseReturnRepository.save(pr);
        return buildResponse(saved, shopId, true);
    }

    @Transactional
    public PurchaseReturnResponse completeReturn(UUID returnId) {
        UUID shopId = SecurityContextUtil.getCurrentShopId();
        PurchaseReturn pr = purchaseReturnRepository.findByIdAndShopIdAndIsActiveTrue(returnId, shopId)
                .orElseThrow(() -> new IllegalArgumentException("Purchase return not found"));

        if (pr.getReturnStatus() != PurchaseReturnStatus.SUBMITTED) {
            throw new IllegalArgumentException("Only SUBMITTED returns can be completed");
        }

        GoodsReceivedNote grn = grnRepository.findByIdAndShopIdAndIsActiveTrue(pr.getGrnId(), shopId)
                .orElseThrow(() -> new IllegalArgumentException("GRN not found"));

        Map<UUID, GoodsReceivedNoteItem> grnItemMap = grn.getItems().stream()
                .collect(Collectors.toMap(GoodsReceivedNoteItem::getId, Function.identity()));

        // Idempotency guard — only deduct stock once
        if (!pr.getStockDeducted()) {
            for (PurchaseReturnItem item : pr.getItems()) {
                GoodsReceivedNoteItem grnItem = grnItemMap.get(item.getGrnItemId());
                if (grnItem == null) {
                    throw new IllegalArgumentException("GRN item not found: " + item.getGrnItemId());
                }

                // Update GRN item returned quantity
                BigDecimal currentReturned = grnItem.getReturnedQuantity() != null ? grnItem.getReturnedQuantity() : BigDecimal.ZERO;
                BigDecimal newReturned = currentReturned.add(item.getQuantityReturned());
                if (newReturned.compareTo(grnItem.getQuantityReceived()) > 0) {
                    throw new IllegalArgumentException(
                            "Cannot return more than received for " + item.getProductName()
                                    + " (" + item.getSku() + "). Received: " + grnItem.getQuantityReceived()
                                    + ", Already returned: " + grnItem.getReturnedQuantity()
                                    + ", Attempting to return: " + item.getQuantityReturned());
                }
                grnItem.setReturnedQuantity(newReturned);

                // Deduct from inventory stock
                InventoryStock stock = inventoryStockRepository
                        .findByVariantIdAndShopIdAndIsActiveTrue(item.getVariantId(), shopId)
                        .orElse(null);

                if (stock != null) {
                    BigDecimal newQty = stock.getCurrentQuantity().subtract(item.getQuantityReturned());
                    stock.setCurrentQuantity(newQty.max(BigDecimal.ZERO));
                    inventoryStockRepository.save(stock);
                }
            }

            grnRepository.save(grn);
            pr.setStockDeducted(true);
        }

        pr.setReturnStatus(PurchaseReturnStatus.COMPLETED);
        pr.setCompletedAt(LocalDateTime.now());

        PurchaseReturn saved = purchaseReturnRepository.save(pr);
        return buildResponse(saved, shopId, true);
    }

    @Transactional
    public PurchaseReturnResponse cancelReturn(UUID returnId) {
        UUID shopId = SecurityContextUtil.getCurrentShopId();
        PurchaseReturn pr = purchaseReturnRepository.findByIdAndShopIdAndIsActiveTrue(returnId, shopId)
                .orElseThrow(() -> new IllegalArgumentException("Purchase return not found"));

        if (pr.getReturnStatus() == PurchaseReturnStatus.CANCELLED) {
            throw new IllegalArgumentException("Purchase return is already cancelled");
        }

        // If stock was deducted, reverse it
        if (pr.getStockDeducted()) {
            GoodsReceivedNote grn = grnRepository.findByIdAndShopIdAndIsActiveTrue(pr.getGrnId(), shopId)
                    .orElseThrow(() -> new IllegalArgumentException("GRN not found"));

            Map<UUID, GoodsReceivedNoteItem> grnItemMap = grn.getItems().stream()
                    .collect(Collectors.toMap(GoodsReceivedNoteItem::getId, Function.identity()));

            for (PurchaseReturnItem item : pr.getItems()) {
                // Reverse GRN item returned quantity
                GoodsReceivedNoteItem grnItem = grnItemMap.get(item.getGrnItemId());
                if (grnItem != null) {
                    BigDecimal currentReturned = grnItem.getReturnedQuantity() != null ? grnItem.getReturnedQuantity() : BigDecimal.ZERO;
                    BigDecimal reversed = currentReturned.subtract(item.getQuantityReturned());
                    grnItem.setReturnedQuantity(reversed.max(BigDecimal.ZERO));
                }

                // Restore inventory stock
                InventoryStock stock = inventoryStockRepository
                        .findByVariantIdAndShopIdAndIsActiveTrue(item.getVariantId(), shopId)
                        .orElse(null);

                if (stock != null) {
                    stock.setCurrentQuantity(stock.getCurrentQuantity().add(item.getQuantityReturned()));
                    stock.setLastRestockedAt(LocalDateTime.now());
                    inventoryStockRepository.save(stock);
                }
            }

            grnRepository.save(grn);
            pr.setStockDeducted(false);
        }

        pr.setReturnStatus(PurchaseReturnStatus.CANCELLED);
        pr.setCancelledAt(LocalDateTime.now());

        PurchaseReturn saved = purchaseReturnRepository.save(pr);
        return buildResponse(saved, shopId, true);
    }

    // ==================== DELETE ====================

    @Transactional
    public void delete(UUID id) {
        UUID shopId = SecurityContextUtil.getCurrentShopId();
        PurchaseReturn pr = purchaseReturnRepository.findByIdAndShopIdAndIsActiveTrue(id, shopId)
                .orElseThrow(() -> new IllegalArgumentException("Purchase return not found"));

        if (pr.getReturnStatus() != PurchaseReturnStatus.DRAFT
                && pr.getReturnStatus() != PurchaseReturnStatus.CANCELLED) {
            throw new IllegalArgumentException("Only DRAFT or CANCELLED returns can be deleted");
        }

        pr.setIsActive(false);
        purchaseReturnRepository.save(pr);
    }

    @Transactional
    public int bulkDelete(List<UUID> ids) {
        UUID shopId = SecurityContextUtil.getCurrentShopId();
        List<PurchaseReturn> returns = purchaseReturnRepository.findAllByIdInAndShopIdAndIsActiveTrue(ids, shopId);
        if (returns.isEmpty()) {
            throw new IllegalArgumentException("No purchase returns found for the given IDs");
        }

        for (PurchaseReturn pr : returns) {
            if (pr.getReturnStatus() != PurchaseReturnStatus.DRAFT
                    && pr.getReturnStatus() != PurchaseReturnStatus.CANCELLED) {
                throw new IllegalArgumentException(
                        "Return " + pr.getReturnNumber() + " cannot be deleted — only DRAFT or CANCELLED returns");
            }
            pr.setIsActive(false);
        }
        purchaseReturnRepository.saveAll(returns);
        return returns.size();
    }

    // ==================== CREATE / UPDATE ====================

    private PurchaseReturnResponse createReturn(PurchaseReturnRequest request, UUID shopId) {
        // Validate GRN exists and is completed
        GoodsReceivedNote grn = grnRepository.findByIdAndShopIdAndIsActiveTrue(request.getGrnId(), shopId)
                .orElseThrow(() -> new IllegalArgumentException("GRN not found"));

        if (grn.getGrnStatus() != GrnStatus.COMPLETED) {
            throw new IllegalArgumentException("Can only create returns against COMPLETED GRNs");
        }

        // Derive supplier and PO from GRN
        PurchaseOrder po = purchaseOrderRepository.findByIdAndShopIdAndIsActiveTrue(grn.getPurchaseOrderId(), shopId)
                .orElseThrow(() -> new IllegalArgumentException("Purchase order not found for GRN"));

        UUID supplierId = po.getSupplierId();

        // Validate supplier exists
        supplierRepository.findByIdAndShopIdAndIsActiveTrue(supplierId, shopId)
                .orElseThrow(() -> new IllegalArgumentException("Supplier not found"));

        // Build GRN item map keyed by variantId for lookup by product
        Map<UUID, GoodsReceivedNoteItem> grnItemByVariant = grn.getItems().stream()
                .collect(Collectors.toMap(GoodsReceivedNoteItem::getVariantId, Function.identity()));

        // Generate return number
        String returnNumber = generateReturnNumber(shopId);

        PurchaseReturn pr = PurchaseReturn.builder()
                .shopId(shopId)
                .returnNumber(returnNumber)
                .supplierId(supplierId)
                .grnId(grn.getId())
                .purchaseOrderId(grn.getPurchaseOrderId())
                .returnDate(request.getReturnDate() != null ? request.getReturnDate() : LocalDate.now())
                .referenceNumber(request.getReferenceNumber())
                .reason(request.getReason())
                .notes(request.getNotes())
                .build();

        // Build return items
        List<PurchaseReturnItem> items = new ArrayList<>();
        BigDecimal totalAmount = BigDecimal.ZERO;
        for (PurchaseReturnItemRequest itemReq : request.getItems()) {
            PurchaseReturnItem item = buildReturnItem(itemReq, pr, grnItemByVariant);
            items.add(item);
            totalAmount = totalAmount.add(item.getTotalCost());
        }
        pr.setItems(items);
        pr.setTotalAmount(totalAmount);

        PurchaseReturn saved = purchaseReturnRepository.save(pr);
        return buildResponse(saved, shopId, true);
    }

    private PurchaseReturnResponse updateReturn(PurchaseReturnRequest request, UUID shopId) {
        PurchaseReturn pr = purchaseReturnRepository.findByIdAndShopIdAndIsActiveTrue(request.getId(), shopId)
                .orElseThrow(() -> new IllegalArgumentException("Purchase return not found"));

        if (pr.getReturnStatus() != PurchaseReturnStatus.DRAFT) {
            throw new IllegalArgumentException("Only DRAFT returns can be updated");
        }

        GoodsReceivedNote grn = grnRepository.findByIdAndShopIdAndIsActiveTrue(pr.getGrnId(), shopId)
                .orElseThrow(() -> new IllegalArgumentException("GRN not found"));

        Map<UUID, GoodsReceivedNoteItem> grnItemByVariant = grn.getItems().stream()
                .collect(Collectors.toMap(GoodsReceivedNoteItem::getVariantId, Function.identity()));

        pr.setReturnDate(request.getReturnDate() != null ? request.getReturnDate() : pr.getReturnDate());
        pr.setReferenceNumber(request.getReferenceNumber());
        pr.setReason(request.getReason());
        pr.setNotes(request.getNotes());

        // Rebuild items
        pr.getItems().clear();
        BigDecimal totalAmount = BigDecimal.ZERO;
        for (PurchaseReturnItemRequest itemReq : request.getItems()) {
            PurchaseReturnItem item = buildReturnItem(itemReq, pr, grnItemByVariant);
            pr.getItems().add(item);
            totalAmount = totalAmount.add(item.getTotalCost());
        }
        pr.setTotalAmount(totalAmount);

        PurchaseReturn saved = purchaseReturnRepository.save(pr);
        return buildResponse(saved, shopId, true);
    }

    // ==================== ITEM BUILDING ====================

    private PurchaseReturnItem buildReturnItem(PurchaseReturnItemRequest itemReq,
                                                PurchaseReturn pr,
                                                Map<UUID, GoodsReceivedNoteItem> grnItemByVariant) {
        GoodsReceivedNoteItem grnItem = grnItemByVariant.get(itemReq.getVariantId());
        if (grnItem == null) {
            throw new IllegalArgumentException("Variant not found in GRN: " + itemReq.getVariantId());
        }

        // Validate: quantityReturned <= (quantityReceived - alreadyReturned)
        BigDecimal alreadyReturned = grnItem.getReturnedQuantity() != null ? grnItem.getReturnedQuantity() : BigDecimal.ZERO;
        BigDecimal returnable = grnItem.getQuantityReceived().subtract(alreadyReturned);
        if (itemReq.getQuantityReturned().compareTo(returnable) > 0) {
            throw new IllegalArgumentException(
                    "Cannot return " + itemReq.getQuantityReturned() + " of "
                            + grnItem.getProductName() + " (" + grnItem.getSku()
                            + "). Returnable quantity: " + returnable
                            + " (Received: " + grnItem.getQuantityReceived()
                            + ", Already returned: " + grnItem.getReturnedQuantity() + ")");
        }

        BigDecimal unitCost = itemReq.getUnitCost() != null ? itemReq.getUnitCost() : grnItem.getUnitCost();
        BigDecimal totalCost = unitCost.multiply(itemReq.getQuantityReturned());

        return PurchaseReturnItem.builder()
                .purchaseReturn(pr)
                .grnItemId(grnItem.getId())
                .variantId(grnItem.getVariantId())
                .productName(grnItem.getProductName())
                .variantName(grnItem.getVariantName())
                .sku(grnItem.getSku())
                .quantityReturned(itemReq.getQuantityReturned())
                .unitCost(unitCost)
                .totalCost(totalCost)
                .returnReason(itemReq.getReturnReason())
                .notes(itemReq.getNotes())
                .build();
    }

    // ==================== RESPONSE BUILDER ====================

    private PurchaseReturnResponse buildResponse(PurchaseReturn pr, UUID shopId) {
        return buildResponse(pr, shopId, false);
    }

    private PurchaseReturnResponse buildResponse(PurchaseReturn pr, UUID shopId, boolean includeItems) {
        String supplierName = null;
        String grnNumber = null;
        String poNumber = null;

        // Resolve supplier name
        Supplier supplier = supplierRepository.findByIdAndShopIdAndIsActiveTrue(pr.getSupplierId(), shopId)
                .orElse(null);
        if (supplier != null) {
            supplierName = supplier.getSupplierName();
        }

        // Resolve GRN number
        GoodsReceivedNote grn = grnRepository.findByIdAndShopIdAndIsActiveTrue(pr.getGrnId(), shopId)
                .orElse(null);
        if (grn != null) {
            grnNumber = grn.getGrnNumber();
        }

        // Resolve PO number
        if (pr.getPurchaseOrderId() != null) {
            PurchaseOrder po = purchaseOrderRepository.findByIdAndShopIdAndIsActiveTrue(pr.getPurchaseOrderId(), shopId)
                    .orElse(null);
            if (po != null) {
                poNumber = po.getPoNumber();
            }
        }

        // Build items only when requested (single fetch, save, status transitions)
        List<PurchaseReturnItemResponse> itemResponses = null;
        if (includeItems) {
            Map<UUID, GoodsReceivedNoteItem> grnItemMap = grn != null
                    ? grn.getItems().stream().collect(Collectors.toMap(GoodsReceivedNoteItem::getId, Function.identity()))
                    : Map.of();

            itemResponses = pr.getItems().stream()
                    .map(item -> {
                        GoodsReceivedNoteItem grnItem = grnItemMap.get(item.getGrnItemId());
                        BigDecimal quantityReceived = grnItem != null ? grnItem.getQuantityReceived() : null;
                        BigDecimal quantityAlreadyReturned = grnItem != null ? grnItem.getReturnedQuantity() : null;

                        return PurchaseReturnItemResponse.builder()
                                .id(item.getId())
                                .grnItemId(item.getGrnItemId())
                                .variantId(item.getVariantId())
                                .productName(item.getProductName())
                                .variantName(item.getVariantName())
                                .sku(item.getSku())
                                .quantityReceived(quantityReceived)
                                .quantityAlreadyReturned(quantityAlreadyReturned)
                                .quantityReturned(item.getQuantityReturned())
                                .unitCost(item.getUnitCost())
                                .totalCost(item.getTotalCost())
                                .returnReason(item.getReturnReason())
                                .notes(item.getNotes())
                                .build();
                    })
                    .toList();
        }

        return PurchaseReturnResponse.builder()
                .id(pr.getId())
                .returnNumber(pr.getReturnNumber())
                .supplierId(pr.getSupplierId())
                .supplierName(supplierName)
                .grnId(pr.getGrnId())
                .grnNumber(grnNumber)
                .purchaseOrderId(pr.getPurchaseOrderId())
                .poNumber(poNumber)
                .returnStatus(pr.getReturnStatus())
                .returnDate(pr.getReturnDate())
                .returnedBy(pr.getReturnedBy())
                .referenceNumber(pr.getReferenceNumber())
                .reason(pr.getReason())
                .notes(pr.getNotes())
                .totalAmount(pr.getTotalAmount())
                .submittedAt(pr.getSubmittedAt())
                .completedAt(pr.getCompletedAt())
                .cancelledAt(pr.getCancelledAt())
                .createdAt(pr.getCreatedAt())
                .updatedAt(pr.getUpdatedAt())
                .items(itemResponses)
                .build();
    }

    // ==================== HELPERS ====================

    private String generateReturnNumber(UUID shopId) {
        long count = purchaseReturnRepository.countByShopId(shopId);
        String code;
        do {
            count++;
            code = String.format("PR-%05d", count);
        } while (purchaseReturnRepository.existsByShopIdAndReturnNumber(shopId, code));
        return code;
    }

}
