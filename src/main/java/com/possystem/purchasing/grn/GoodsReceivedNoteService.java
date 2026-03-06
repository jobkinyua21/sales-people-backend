package com.possystem.purchasing.grn;

import com.possystem.common.ListResponse;
import com.possystem.inventory.InventoryStock;
import com.possystem.inventory.InventoryStockRepository;
import com.possystem.purchasing.enums.GrnStatus;
import com.possystem.purchasing.enums.PurchaseOrderStatus;
import com.possystem.purchasing.order.PurchaseOrder;
import com.possystem.purchasing.order.PurchaseOrderItem;
import com.possystem.purchasing.order.PurchaseOrderRepository;
import com.possystem.purchasing.order.PurchaseOrderService;
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
public class GoodsReceivedNoteService {

    private final GoodsReceivedNoteRepository grnRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final PurchaseOrderService purchaseOrderService;
    private final InventoryStockRepository inventoryStockRepository;
    private final SupplierRepository supplierRepository;

    // ==================== CRUD ====================

    @Transactional
    public GrnResponse save(GrnRequest request) {
        UUID shopId = SecurityContextUtil.getCurrentShopId();

        if (request.getId() != null) {
            return updateGrn(request, shopId);
        }
        return createGrn(request, shopId);
    }

    public ListResponse<GrnResponse> fetch(GrnFetchRequest request) {
        UUID shopId = SecurityContextUtil.getCurrentShopId();

        if (request.getId() != null) {
            GoodsReceivedNote grn = grnRepository.findByIdAndShopIdAndIsActiveTrue(request.getId(), shopId)
                    .orElseThrow(() -> new IllegalArgumentException("GRN not found"));
            List<GrnResponse> result = List.of(buildResponse(grn, shopId));
            return ListResponse.of(result);
        }

        String search = request.getSearch();
        String grnStatus = request.getGrnStatus() != null ? request.getGrnStatus().name() : null;
        UUID supplierId = request.getSupplierId();
        UUID purchaseOrderId = request.getPurchaseOrderId();
        LocalDateTime dateFrom = request.getDateFrom();
        LocalDateTime dateTo = request.getDateTo();
        Integer limit = request.getLimit();

        if (limit == null) {
            List<GoodsReceivedNote> all = grnRepository.searchFilteredUnpaged(
                    shopId, search, grnStatus, supplierId, purchaseOrderId, dateFrom, dateTo);
            List<GrnResponse> responses = all.stream()
                    .map(g -> buildResponse(g, shopId))
                    .toList();
            return ListResponse.of(responses);
        }

        PageRequest pageRequest = PageRequest.of(request.getStart(), limit);
        Page<GoodsReceivedNote> page = grnRepository.searchFiltered(
                shopId, search, grnStatus, supplierId, purchaseOrderId, dateFrom, dateTo, pageRequest);
        Page<GrnResponse> responsePage = page.map(g -> buildResponse(g, shopId));
        return ListResponse.from(responsePage);
    }

    // ==================== STATUS TRANSITIONS ====================

    @Transactional
    public GrnResponse completeGrn(UUID grnId) {
        UUID shopId = SecurityContextUtil.getCurrentShopId();
        GoodsReceivedNote grn = grnRepository.findByIdAndShopIdAndIsActiveTrue(grnId, shopId)
                .orElseThrow(() -> new IllegalArgumentException("GRN not found"));

        if (grn.getGrnStatus() != GrnStatus.PENDING) {
            throw new IllegalArgumentException("Only PENDING GRNs can be completed");
        }

        PurchaseOrder po = purchaseOrderRepository.findByIdAndShopIdAndIsActiveTrue(grn.getPurchaseOrderId(), shopId)
                .orElseThrow(() -> new IllegalArgumentException("Purchase order not found"));

        // Build a map of PO items for quick lookup
        Map<UUID, PurchaseOrderItem> poItemMap = po.getItems().stream()
                .collect(Collectors.toMap(PurchaseOrderItem::getId, Function.identity()));

        // Idempotency guard — only update stock once
        if (!grn.getStockUpdated()) {
            for (GoodsReceivedNoteItem grnItem : grn.getItems()) {
                PurchaseOrderItem poItem = poItemMap.get(grnItem.getPurchaseOrderItemId());
                if (poItem == null) {
                    throw new IllegalArgumentException("PO item not found: " + grnItem.getPurchaseOrderItemId());
                }

                // Update PO item received and damaged quantities
                BigDecimal newReceived = poItem.getReceivedQuantity().add(grnItem.getQuantityReceived());
                BigDecimal newDamaged = poItem.getDamagedQuantity().add(grnItem.getQuantityDamaged());
                BigDecimal totalAccounted = newReceived.add(newDamaged);

                if (totalAccounted.compareTo(poItem.getQuantity()) > 0) {
                    throw new IllegalArgumentException(
                            "Cannot over-receive " + poItem.getProductName() + " " + poItem.getVariantName()
                                    + ". Ordered: " + poItem.getQuantity()
                                    + ", Total accounted (received + damaged): " + totalAccounted);
                }
                poItem.setReceivedQuantity(newReceived);
                poItem.setDamagedQuantity(newDamaged);

                // Increment inventory stock
                InventoryStock stock = inventoryStockRepository
                        .findByVariantIdAndShopIdAndIsActiveTrue(grnItem.getVariantId(), shopId)
                        .orElse(null);

                if (stock != null) {
                    stock.setCurrentQuantity(stock.getCurrentQuantity().add(grnItem.getQuantityReceived()));
                    stock.setLastRestockedAt(LocalDateTime.now());
                    inventoryStockRepository.save(stock);
                } else {
                    // Create new stock record if it doesn't exist
                    InventoryStock newStock = InventoryStock.builder()
                            .shopId(shopId)
                            .variantId(grnItem.getVariantId())
                            .currentQuantity(grnItem.getQuantityReceived())
                            .lastRestockedAt(LocalDateTime.now())
                            .build();
                    inventoryStockRepository.save(newStock);
                }
            }

            grn.setStockUpdated(true);
        }

        // Save PO with updated received quantities
        purchaseOrderRepository.save(po);

        // Update PO status (PARTIALLY_RECEIVED or RECEIVED)
        purchaseOrderService.updateReceivingStatus(po.getId(), shopId);

        grn.setGrnStatus(GrnStatus.COMPLETED);
        grn.setCompletedAt(LocalDateTime.now());

        GoodsReceivedNote saved = grnRepository.save(grn);
        return buildResponse(saved, shopId);
    }

    @Transactional
    public GrnResponse cancelGrn(UUID grnId) {
        UUID shopId = SecurityContextUtil.getCurrentShopId();
        GoodsReceivedNote grn = grnRepository.findByIdAndShopIdAndIsActiveTrue(grnId, shopId)
                .orElseThrow(() -> new IllegalArgumentException("GRN not found"));

        if (grn.getGrnStatus() == GrnStatus.CANCELLED) {
            throw new IllegalArgumentException("GRN is already cancelled");
        }

        // If stock was already updated, reverse it
        if (grn.getStockUpdated()) {
            PurchaseOrder po = purchaseOrderRepository.findByIdAndShopIdAndIsActiveTrue(grn.getPurchaseOrderId(), shopId)
                    .orElseThrow(() -> new IllegalArgumentException("Purchase order not found"));

            Map<UUID, PurchaseOrderItem> poItemMap = po.getItems().stream()
                    .collect(Collectors.toMap(PurchaseOrderItem::getId, Function.identity()));

            for (GoodsReceivedNoteItem grnItem : grn.getItems()) {
                // Reverse PO item received and damaged quantities
                PurchaseOrderItem poItem = poItemMap.get(grnItem.getPurchaseOrderItemId());
                if (poItem != null) {
                    BigDecimal reversed = poItem.getReceivedQuantity().subtract(grnItem.getQuantityReceived());
                    poItem.setReceivedQuantity(reversed.max(BigDecimal.ZERO));
                    BigDecimal reversedDamaged = poItem.getDamagedQuantity().subtract(grnItem.getQuantityDamaged());
                    poItem.setDamagedQuantity(reversedDamaged.max(BigDecimal.ZERO));
                }

                // Reverse inventory stock
                InventoryStock stock = inventoryStockRepository
                        .findByVariantIdAndShopIdAndIsActiveTrue(grnItem.getVariantId(), shopId)
                        .orElse(null);

                if (stock != null) {
                    BigDecimal newQty = stock.getCurrentQuantity().subtract(grnItem.getQuantityReceived());
                    stock.setCurrentQuantity(newQty.max(BigDecimal.ZERO));
                    inventoryStockRepository.save(stock);
                }
            }

            purchaseOrderRepository.save(po);
            purchaseOrderService.updateReceivingStatus(po.getId(), shopId);

            grn.setStockUpdated(false);
        }

        grn.setGrnStatus(GrnStatus.CANCELLED);
        grn.setCancelledAt(LocalDateTime.now());

        GoodsReceivedNote saved = grnRepository.save(grn);
        return buildResponse(saved, shopId);
    }

    // ==================== CREATE / UPDATE ====================

    private GrnResponse createGrn(GrnRequest request, UUID shopId) {
        // Validate PO exists and is in receivable state
        PurchaseOrder po = purchaseOrderRepository.findByIdAndShopIdAndIsActiveTrue(request.getPurchaseOrderId(), shopId)
                .orElseThrow(() -> new IllegalArgumentException("Purchase order not found"));

        if (po.getOrderStatus() != PurchaseOrderStatus.ORDERED
                && po.getOrderStatus() != PurchaseOrderStatus.PARTIALLY_RECEIVED) {
            throw new IllegalArgumentException("Purchase order must be ORDERED or PARTIALLY_RECEIVED to receive stock");
        }

        // Build PO item map for validation
        Map<UUID, PurchaseOrderItem> poItemMap = po.getItems().stream()
                .collect(Collectors.toMap(PurchaseOrderItem::getId, Function.identity()));

        // Generate GRN number
        long count = grnRepository.countByShopId(shopId);
        String grnNumber = String.format("GRN-%05d", count + 1);

        GoodsReceivedNote grn = GoodsReceivedNote.builder()
                .shopId(shopId)
                .grnNumber(grnNumber)
                .purchaseOrderId(request.getPurchaseOrderId())
                .receivedDate(request.getReceivedDate() != null ? request.getReceivedDate() : LocalDate.now())
                .receivedBy(SecurityContextUtil.getCurrentUserId())
                .referenceNumber(request.getReferenceNumber())
                .notes(request.getNotes())
                .build();

        // Build GRN items
        List<GoodsReceivedNoteItem> items = new ArrayList<>();
        for (GrnItemRequest itemReq : request.getItems()) {
            items.add(buildGrnItem(itemReq, grn, poItemMap));
        }
        grn.setItems(items);

        GoodsReceivedNote saved = grnRepository.save(grn);
        return buildResponse(saved, shopId);
    }

    private GrnResponse updateGrn(GrnRequest request, UUID shopId) {
        GoodsReceivedNote grn = grnRepository.findByIdAndShopIdAndIsActiveTrue(request.getId(), shopId)
                .orElseThrow(() -> new IllegalArgumentException("GRN not found"));

        if (grn.getGrnStatus() != GrnStatus.PENDING) {
            throw new IllegalArgumentException("Only PENDING GRNs can be updated");
        }

        PurchaseOrder po = purchaseOrderRepository.findByIdAndShopIdAndIsActiveTrue(grn.getPurchaseOrderId(), shopId)
                .orElseThrow(() -> new IllegalArgumentException("Purchase order not found"));

        Map<UUID, PurchaseOrderItem> poItemMap = po.getItems().stream()
                .collect(Collectors.toMap(PurchaseOrderItem::getId, Function.identity()));

        grn.setReceivedDate(request.getReceivedDate() != null ? request.getReceivedDate() : grn.getReceivedDate());
        grn.setReferenceNumber(request.getReferenceNumber());
        grn.setNotes(request.getNotes());

        // Rebuild items
        grn.getItems().clear();
        for (GrnItemRequest itemReq : request.getItems()) {
            grn.getItems().add(buildGrnItem(itemReq, grn, poItemMap));
        }

        GoodsReceivedNote saved = grnRepository.save(grn);
        return buildResponse(saved, shopId);
    }

    private GoodsReceivedNoteItem buildGrnItem(GrnItemRequest itemReq, GoodsReceivedNote grn,
                                                Map<UUID, PurchaseOrderItem> poItemMap) {
        PurchaseOrderItem poItem = poItemMap.get(itemReq.getPurchaseOrderItemId());
        if (poItem == null) {
            throw new IllegalArgumentException("PO item not found: " + itemReq.getPurchaseOrderItemId());
        }

        BigDecimal damaged = itemReq.getQuantityDamaged() != null ? itemReq.getQuantityDamaged() : BigDecimal.ZERO;
        BigDecimal missing = itemReq.getQuantityMissing() != null ? itemReq.getQuantityMissing() : BigDecimal.ZERO;

        // Validate: received + damaged + missing cannot exceed remaining ordered quantity
        // Remaining = ordered - (already received + already damaged)
        BigDecimal alreadyAccounted = poItem.getReceivedQuantity().add(poItem.getDamagedQuantity());
        BigDecimal remaining = poItem.getQuantity().subtract(alreadyAccounted);
        BigDecimal totalNew = itemReq.getQuantityReceived().add(damaged).add(missing);

        if (totalNew.compareTo(remaining) > 0) {
            throw new IllegalArgumentException(
                    "Total (received + damaged + missing = " + totalNew + ") exceeds remaining quantity for "
                            + poItem.getProductName() + " " + poItem.getVariantName()
                            + ". Ordered: " + poItem.getQuantity()
                            + ", Already accounted: " + alreadyAccounted
                            + ", Remaining: " + remaining);
        }

        BigDecimal unitCost = itemReq.getUnitCost() != null ? itemReq.getUnitCost() : poItem.getUnitCost();
        BigDecimal totalCost = unitCost.multiply(itemReq.getQuantityReceived());

        return GoodsReceivedNoteItem.builder()
                .goodsReceivedNote(grn)
                .purchaseOrderItemId(poItem.getId())
                .variantId(poItem.getVariantId())
                .productName(poItem.getProductName())
                .variantName(poItem.getVariantName())
                .sku(poItem.getSku())
                .quantityReceived(itemReq.getQuantityReceived())
                .quantityDamaged(damaged)
                .quantityMissing(missing)
                .unitCost(unitCost)
                .totalCost(totalCost)
                .notes(itemReq.getNotes())
                .build();
    }

    // ==================== RESPONSE BUILDER ====================

    private GrnResponse buildResponse(GoodsReceivedNote grn, UUID shopId) {
        String poNumber = null;
        String supplierName = null;

        PurchaseOrder po = purchaseOrderRepository.findByIdAndShopIdAndIsActiveTrue(grn.getPurchaseOrderId(), shopId)
                .orElse(null);
        if (po != null) {
            poNumber = po.getPoNumber();
            Supplier supplier = supplierRepository.findByIdAndShopIdAndIsActiveTrue(po.getSupplierId(), shopId)
                    .orElse(null);
            if (supplier != null) {
                supplierName = supplier.getSupplierName();
            }
        }

        // Build PO item map for ordered quantity lookup
        Map<UUID, PurchaseOrderItem> poItemMap = po != null
                ? po.getItems().stream().collect(Collectors.toMap(PurchaseOrderItem::getId, Function.identity()))
                : Map.of();

        List<GrnItemResponse> itemResponses = grn.getItems().stream()
                .map(item -> {
                    PurchaseOrderItem poItem = poItemMap.get(item.getPurchaseOrderItemId());
                    BigDecimal quantityOrdered = poItem != null ? poItem.getQuantity() : null;

                    return GrnItemResponse.builder()
                            .id(item.getId())
                            .purchaseOrderItemId(item.getPurchaseOrderItemId())
                            .variantId(item.getVariantId())
                            .productName(item.getProductName())
                            .variantName(item.getVariantName())
                            .sku(item.getSku())
                            .quantityOrdered(quantityOrdered)
                            .quantityReceived(item.getQuantityReceived())
                            .quantityDamaged(item.getQuantityDamaged())
                            .quantityMissing(item.getQuantityMissing())
                            .quantityReturned(item.getReturnedQuantity() != null ? item.getReturnedQuantity() : BigDecimal.ZERO)
                            .unitCost(item.getUnitCost())
                            .totalCost(item.getTotalCost())
                            .notes(item.getNotes())
                            .build();
                })
                .toList();

        return GrnResponse.builder()
                .id(grn.getId())
                .grnNumber(grn.getGrnNumber())
                .purchaseOrderId(grn.getPurchaseOrderId())
                .poNumber(poNumber)
                .supplierName(supplierName)
                .grnStatus(grn.getGrnStatus())
                .receivedDate(grn.getReceivedDate())
                .receivedBy(grn.getReceivedBy())
                .referenceNumber(grn.getReferenceNumber())
                .notes(grn.getNotes())
                .completedAt(grn.getCompletedAt())
                .cancelledAt(grn.getCancelledAt())
                .createdAt(grn.getCreatedAt())
                .updatedAt(grn.getUpdatedAt())
                .items(itemResponses)
                .build();
    }

}
