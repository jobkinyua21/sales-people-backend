package com.possystem.kitchen.kot;

import com.possystem.auth.user.User;
import com.possystem.auth.user.UserRepository;
import com.possystem.inventory.InventoryStock;
import com.possystem.inventory.InventoryStockRepository;
import com.possystem.inventory.stockalert.StockAlertService;
import com.possystem.kitchen.recipe.ProductionType;
import com.possystem.kitchen.recipe.Recipe;
import com.possystem.kitchen.recipe.RecipeRepository;
import com.possystem.kitchen.recipe.RecipeService;
import com.possystem.sales.SalesOrder;
import com.possystem.sales.SalesOrderItem;
import com.possystem.sales.SalesOrderRepository;
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
public class KitchenOrderTicketService {

    private final KitchenOrderTicketRepository kotRepository;
    private final KitchenOrderTicketItemRepository kotItemRepository;
    private final SalesOrderRepository salesOrderRepository;
    private final RecipeRepository recipeRepository;
    private final RecipeService recipeService;
    private final InventoryStockRepository inventoryStockRepository;
    private final StockAlertService stockAlertService;
    private final UserRepository userRepository;

    // ==================== CREATE KOT ====================

    @Transactional
    public KotResponse createKot(KotRequest request) {
        UUID shopId = SecurityContextUtil.getCurrentShopId();
        UUID userId = SecurityContextUtil.getCurrentUserId();

        // Validate order
        SalesOrder order = salesOrderRepository.findByIdAndShopIdAndIsActiveTrue(request.getOrderId(), shopId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));

        KitchenOrderTicket kot = KitchenOrderTicket.builder()
                .shopId(shopId)
                .kotNumber(generateKotNumber(shopId))
                .orderId(order.getId())
                .orderNumber(order.getOrderNumber())
                .tableNumber(order.getTableNumber())
                .priority(request.getPriority() != null ? request.getPriority() : 5)
                .specialInstructions(request.getSpecialInstructions())
                .sentBy(userId)
                .build();

        // Build items
        List<KitchenOrderTicketItem> items = new ArrayList<>();
        for (KotItemRequest itemReq : request.getItems()) {
            SalesOrderItem orderItem = order.getItems().stream()
                    .filter(i -> i.getId().equals(itemReq.getOrderItemId()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Order item not found: " + itemReq.getOrderItemId()));

            // Look up recipe for this variant (if exists)
            Recipe recipe = recipeRepository.findByVariantIdAndShopIdAndIsActiveTrue(orderItem.getVariantId(), shopId)
                    .orElse(null);

            KitchenOrderTicketItem kotItem = KitchenOrderTicketItem.builder()
                    .kitchenOrderTicket(kot)
                    .orderItemId(orderItem.getId())
                    .variantId(orderItem.getVariantId())
                    .productName(orderItem.getProductName())
                    .variantName(orderItem.getVariantName())
                    .quantity(itemReq.getQuantity())
                    .specialInstructions(itemReq.getSpecialInstructions())
                    .recipeId(recipe != null ? recipe.getId() : null)
                    .prepStation(recipe != null ? recipe.getPrepStation() : null)
                    .build();

            items.add(kotItem);
        }

        kot.getItems().addAll(items);

        KitchenOrderTicket saved = kotRepository.save(kot);
        return buildResponse(saved);
    }

    // ==================== ACCEPT KOT (kitchen staff picks it up) ====================

    @Transactional
    public KotResponse acceptKot(UUID kotId) {
        UUID shopId = SecurityContextUtil.getCurrentShopId();
        UUID userId = SecurityContextUtil.getCurrentUserId();

        KitchenOrderTicket kot = kotRepository.findByIdAndShopId(kotId, shopId)
                .orElseThrow(() -> new IllegalArgumentException("KOT not found"));

        if (kot.getStatus() != KotStatus.PENDING) {
            throw new IllegalArgumentException("Only PENDING KOTs can be accepted");
        }

        kot.setStatus(KotStatus.IN_PROGRESS);
        kot.setAcceptedBy(userId);
        kot.setAcceptedAt(LocalDateTime.now());

        // Deduct ingredients for all items that have recipes
        deductIngredientsForKot(kot, shopId);

        KitchenOrderTicket saved = kotRepository.save(kot);
        return buildResponse(saved);
    }

    // ==================== UPDATE ITEM STATUS ====================

    @Transactional
    public KotResponse updateItemStatus(KotUpdateItemStatusRequest request) {
        UUID shopId = SecurityContextUtil.getCurrentShopId();
        UUID userId = SecurityContextUtil.getCurrentUserId();

        KitchenOrderTicketItem item = kotItemRepository.findByIdAndKitchenOrderTicketShopId(request.getItemId(), shopId)
                .orElseThrow(() -> new IllegalArgumentException("KOT item not found"));

        KitchenOrderTicket kot = item.getKitchenOrderTicket();

        switch (request.getStatus()) {
            case PREPARING -> {
                if (item.getStatus() != KotItemStatus.PENDING) {
                    throw new IllegalArgumentException("Only PENDING items can start preparing");
                }
                item.setStatus(KotItemStatus.PREPARING);
                item.setPreparedBy(userId);
                item.setStartedAt(LocalDateTime.now());
            }
            case READY -> {
                if (item.getStatus() != KotItemStatus.PREPARING) {
                    throw new IllegalArgumentException("Only PREPARING items can be marked as ready");
                }
                item.setStatus(KotItemStatus.READY);
                item.setReadyAt(LocalDateTime.now());
            }
            case SERVED -> {
                if (item.getStatus() != KotItemStatus.READY) {
                    throw new IllegalArgumentException("Only READY items can be marked as served");
                }
                item.setStatus(KotItemStatus.SERVED);
                item.setServedAt(LocalDateTime.now());
            }
            case CANCELLED -> {
                if (item.getStatus() == KotItemStatus.SERVED || item.getStatus() == KotItemStatus.CANCELLED) {
                    throw new IllegalArgumentException("Cannot cancel a SERVED or already CANCELLED item");
                }
                item.setStatus(KotItemStatus.CANCELLED);
                item.setCancelledAt(LocalDateTime.now());
                item.setCancelReason(request.getCancelReason());

                // Restore stock if it was already deducted
                if (Boolean.TRUE.equals(kot.getIngredientsDeducted()) && item.getRecipeId() != null) {
                    restoreStockForItem(item, shopId);
                }
            }
            default -> throw new IllegalArgumentException("Invalid status transition");
        }

        kotItemRepository.save(item);

        // Auto-update KOT status based on items
        autoUpdateKotStatus(kot);
        KitchenOrderTicket saved = kotRepository.save(kot);
        return buildResponse(saved);
    }

    // ==================== CANCEL KOT ====================

    @Transactional
    public KotResponse cancelKot(KotCancelRequest request) {
        UUID shopId = SecurityContextUtil.getCurrentShopId();

        KitchenOrderTicket kot = kotRepository.findByIdAndShopId(request.getKotId(), shopId)
                .orElseThrow(() -> new IllegalArgumentException("KOT not found"));

        if (kot.getStatus() == KotStatus.COMPLETED || kot.getStatus() == KotStatus.CANCELLED) {
            throw new IllegalArgumentException("Cannot cancel a COMPLETED or already CANCELLED KOT");
        }

        // Cancel all non-served, non-cancelled items
        for (KitchenOrderTicketItem item : kot.getItems()) {
            if (item.getStatus() != KotItemStatus.SERVED && item.getStatus() != KotItemStatus.CANCELLED) {
                item.setStatus(KotItemStatus.CANCELLED);
                item.setCancelledAt(LocalDateTime.now());
                item.setCancelReason(request.getReason());

                // Restore stock
                if (Boolean.TRUE.equals(kot.getIngredientsDeducted()) && item.getRecipeId() != null) {
                    restoreStockForItem(item, shopId);
                }
            }
        }

        kot.setStatus(KotStatus.CANCELLED);
        kot.setCancelledAt(LocalDateTime.now());
        kot.setCancelReason(request.getReason());

        KitchenOrderTicket saved = kotRepository.save(kot);
        return buildResponse(saved);
    }

    // ==================== QUERIES ====================

    public List<KotResponse> getActiveKots() {
        UUID shopId = SecurityContextUtil.getCurrentShopId();
        List<KotStatus> activeStatuses = List.of(KotStatus.PENDING, KotStatus.IN_PROGRESS);
        return kotRepository.findAllByShopIdAndStatusInOrderByPriorityAscCreatedAtAsc(shopId, activeStatuses)
                .stream().map(this::buildResponse).toList();
    }

    public List<KotResponse> getKotsByOrder(UUID orderId) {
        UUID shopId = SecurityContextUtil.getCurrentShopId();
        return kotRepository.findAllByOrderIdAndShopId(orderId, shopId)
                .stream().map(this::buildResponse).toList();
    }

    public List<KotResponse> getPendingKots() {
        UUID shopId = SecurityContextUtil.getCurrentShopId();
        return kotRepository.findAllByShopIdAndStatusOrderByPriorityAscCreatedAtAsc(shopId, KotStatus.PENDING)
                .stream().map(this::buildResponse).toList();
    }

    public KotResponse getKotById(UUID kotId) {
        UUID shopId = SecurityContextUtil.getCurrentShopId();
        KitchenOrderTicket kot = kotRepository.findByIdAndShopId(kotId, shopId)
                .orElseThrow(() -> new IllegalArgumentException("KOT not found"));
        return buildResponse(kot);
    }

    /**
     * Kitchen Display System — get items filtered by prep station.
     */
    public List<KotResponse.KotItemResponse> getItemsByStation(com.possystem.kitchen.recipe.PrepStation station) {
        UUID shopId = SecurityContextUtil.getCurrentShopId();
        List<KotItemStatus> activeStatuses = List.of(KotItemStatus.PENDING, KotItemStatus.PREPARING);
        return kotItemRepository.findAllByKitchenOrderTicketShopIdAndPrepStationAndStatusInOrderByCreatedAtAsc(
                shopId, station, activeStatuses)
                .stream().map(this::buildItemResponse).toList();
    }

    // ==================== PRIVATE HELPERS ====================

    private void deductIngredientsForKot(KitchenOrderTicket kot, UUID shopId) {
        if (Boolean.TRUE.equals(kot.getIngredientsDeducted())) return;

        for (KitchenOrderTicketItem item : kot.getItems()) {
            if (item.getStatus() == KotItemStatus.CANCELLED) continue;

            if (item.getRecipeId() != null) {
                Recipe recipe = recipeRepository.findByIdAndShopIdAndIsActiveTrue(item.getRecipeId(), shopId)
                        .orElse(null);

                if (recipe != null && recipe.getProductionType() == ProductionType.BATCH_PREP) {
                    // BATCH_PREP: deduct from finished goods stock (chapatis, samosas, etc.)
                    deductFinishedGoodsStock(item.getVariantId(), item.getQuantity(), shopId, item.getProductName());
                } else if (recipe != null) {
                    // COOK_TO_ORDER: deduct raw ingredients via recipe
                    recipeService.deductIngredients(item.getRecipeId(), item.getQuantity(), shopId);
                }
            }
        }

        kot.setIngredientsDeducted(true);
    }

    private void deductFinishedGoodsStock(UUID variantId, BigDecimal quantity, UUID shopId, String productName) {
        InventoryStock stock = inventoryStockRepository.findByVariantIdAndShopIdAndIsActiveTrue(variantId, shopId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No stock record for " + productName + ". Create a production order first."));

        if (stock.getCurrentQuantity().compareTo(quantity) < 0) {
            throw new IllegalArgumentException(
                    "Insufficient prepared stock for " + productName +
                            ". Available: " + stock.getCurrentQuantity() +
                            ", Ordered: " + quantity +
                            ". Create a production order to produce more.");
        }

        stock.setCurrentQuantity(stock.getCurrentQuantity().subtract(quantity));
        inventoryStockRepository.save(stock);
        stockAlertService.checkAndCreateAlert(shopId, variantId);
    }

    private void restoreStockForItem(KitchenOrderTicketItem item, UUID shopId) {
        Recipe recipe = recipeRepository.findByIdAndShopIdAndIsActiveTrue(item.getRecipeId(), shopId).orElse(null);

        if (recipe != null && recipe.getProductionType() == ProductionType.BATCH_PREP) {
            // Restore finished goods stock
            InventoryStock stock = inventoryStockRepository.findByVariantIdAndShopIdAndIsActiveTrue(
                    item.getVariantId(), shopId).orElse(null);
            if (stock != null) {
                stock.setCurrentQuantity(stock.getCurrentQuantity().add(item.getQuantity()));
                inventoryStockRepository.save(stock);
                stockAlertService.checkAndResolveAlert(shopId, item.getVariantId());
            }
        } else {
            // Restore raw ingredients
            recipeService.restoreIngredients(item.getRecipeId(), item.getQuantity(), shopId);
        }
    }

    private void autoUpdateKotStatus(KitchenOrderTicket kot) {
        List<KitchenOrderTicketItem> items = kot.getItems();

        boolean allServedOrCancelled = items.stream()
                .allMatch(i -> i.getStatus() == KotItemStatus.SERVED || i.getStatus() == KotItemStatus.CANCELLED);

        boolean allCancelled = items.stream()
                .allMatch(i -> i.getStatus() == KotItemStatus.CANCELLED);

        if (allCancelled) {
            kot.setStatus(KotStatus.CANCELLED);
            kot.setCancelledAt(LocalDateTime.now());
        } else if (allServedOrCancelled) {
            kot.setStatus(KotStatus.COMPLETED);
            kot.setCompletedAt(LocalDateTime.now());
        }
    }

    private String generateKotNumber(UUID shopId) {
        long count = kotRepository.countByShopId(shopId);
        return String.format("KOT-%04d", count + 1);
    }

    // ==================== RESPONSE BUILDING ====================

    private KotResponse buildResponse(KitchenOrderTicket kot) {
        List<KotResponse.KotItemResponse> items = kot.getItems().stream()
                .map(this::buildItemResponse).toList();

        long pendingCount = kot.getItems().stream().filter(i -> i.getStatus() == KotItemStatus.PENDING).count();
        long preparingCount = kot.getItems().stream().filter(i -> i.getStatus() == KotItemStatus.PREPARING).count();
        long readyCount = kot.getItems().stream().filter(i -> i.getStatus() == KotItemStatus.READY).count();

        KotResponse response = KotResponse.builder()
                .id(kot.getId())
                .shopId(kot.getShopId())
                .kotNumber(kot.getKotNumber())
                .orderId(kot.getOrderId())
                .orderNumber(kot.getOrderNumber())
                .tableNumber(kot.getTableNumber())
                .status(kot.getStatus())
                .priority(kot.getPriority())
                .specialInstructions(kot.getSpecialInstructions())
                .sentBy(kot.getSentBy())
                .acceptedBy(kot.getAcceptedBy())
                .acceptedAt(kot.getAcceptedAt())
                .completedAt(kot.getCompletedAt())
                .cancelledAt(kot.getCancelledAt())
                .cancelReason(kot.getCancelReason())
                .ingredientsDeducted(kot.getIngredientsDeducted())
                .createdAt(kot.getCreatedAt())
                .items(items)
                .totalItems(kot.getItems().size())
                .pendingItems((int) pendingCount)
                .preparingItems((int) preparingCount)
                .readyItems((int) readyCount)
                .build();

        // Resolve names
        response.setSentByName(resolveName(kot.getSentBy()));
        if (kot.getAcceptedBy() != null) {
            response.setAcceptedByName(resolveName(kot.getAcceptedBy()));
        }

        return response;
    }

    private KotResponse.KotItemResponse buildItemResponse(KitchenOrderTicketItem item) {
        KotResponse.KotItemResponse response = KotResponse.KotItemResponse.builder()
                .id(item.getId())
                .orderItemId(item.getOrderItemId())
                .variantId(item.getVariantId())
                .productName(item.getProductName())
                .variantName(item.getVariantName())
                .quantity(item.getQuantity())
                .status(item.getStatus())
                .prepStation(item.getPrepStation())
                .specialInstructions(item.getSpecialInstructions())
                .recipeId(item.getRecipeId())
                .preparedBy(item.getPreparedBy())
                .startedAt(item.getStartedAt())
                .readyAt(item.getReadyAt())
                .servedAt(item.getServedAt())
                .cancelledAt(item.getCancelledAt())
                .cancelReason(item.getCancelReason())
                .build();

        if (item.getPreparedBy() != null) {
            response.setPreparedByName(resolveName(item.getPreparedBy()));
        }

        return response;
    }

    private String resolveName(UUID userId) {
        if (userId == null) return null;
        return userRepository.findById(userId)
                .map(User::getFullName).orElse(null);
    }
}
