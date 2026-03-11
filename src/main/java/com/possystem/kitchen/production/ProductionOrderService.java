package com.possystem.kitchen.production;

import com.possystem.auth.user.User;
import com.possystem.auth.user.UserRepository;
import com.possystem.inventory.InventoryStock;
import com.possystem.inventory.InventoryStockRepository;
import com.possystem.inventory.Product;
import com.possystem.inventory.ProductRepository;
import com.possystem.inventory.ProductVariant;
import com.possystem.inventory.ProductVariantRepository;
import com.possystem.inventory.stockalert.StockAlertService;
import com.possystem.kitchen.recipe.ProductionType;
import com.possystem.kitchen.recipe.Recipe;
import com.possystem.kitchen.recipe.RecipeIngredient;
import com.possystem.kitchen.recipe.RecipeRepository;
import com.possystem.kitchen.recipe.RecipeStatus;
import com.possystem.security.SecurityContextUtil;
import lombok.RequiredArgsConstructor;
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
public class ProductionOrderService {

    private final ProductionOrderRepository productionOrderRepository;
    private final RecipeRepository recipeRepository;
    private final ProductVariantRepository productVariantRepository;
    private final ProductRepository productRepository;
    private final InventoryStockRepository inventoryStockRepository;
    private final StockAlertService stockAlertService;
    private final UserRepository userRepository;

    // ==================== CREATE PRODUCTION ORDER ====================

    @Transactional
    public ProductionOrderResponse create(ProductionOrderRequest request) {
        UUID shopId = SecurityContextUtil.getCurrentShopId();
        UUID userId = SecurityContextUtil.getCurrentUserId();

        Recipe recipe = recipeRepository.findByIdAndShopIdAndIsActiveTrue(request.getRecipeId(), shopId)
                .orElseThrow(() -> new IllegalArgumentException("Recipe not found"));

        if (recipe.getStatus() != RecipeStatus.ACTIVE) {
            throw new IllegalArgumentException("Recipe must be ACTIVE to create a production order");
        }

        if (recipe.getProductionType() != ProductionType.BATCH_PREP) {
            throw new IllegalArgumentException("Only BATCH_PREP recipes can have production orders. This recipe is COOK_TO_ORDER.");
        }

        if (request.getQuantityToProduce().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Quantity to produce must be greater than zero");
        }

        // Get product info for snapshot
        ProductVariant variant = productVariantRepository.findByIdAndShopIdAndIsActiveTrue(recipe.getVariantId(), shopId)
                .orElseThrow(() -> new IllegalArgumentException("Product variant not found for recipe"));
        Product product = productRepository.findById(variant.getProductId()).orElse(null);

        // Calculate estimated cost
        BigDecimal estimatedCost = calculateProductionCost(recipe, request.getQuantityToProduce());

        ProductionOrder order = ProductionOrder.builder()
                .shopId(shopId)
                .productionNumber(generateProductionNumber(shopId))
                .recipeId(recipe.getId())
                .recipeName(recipe.getRecipeName())
                .productName(product != null ? product.getProductName() : variant.getVariantName())
                .variantName(variant.getVariantName())
                .variantId(recipe.getVariantId())
                .quantityToProduce(request.getQuantityToProduce())
                .yieldUnit(recipe.getYieldUnit())
                .estimatedCost(estimatedCost)
                .notes(request.getNotes())
                .requestedBy(userId)
                .build();

        ProductionOrder saved = productionOrderRepository.save(order);
        return buildResponse(saved, recipe);
    }

    // ==================== START PRODUCTION (ingredients deducted) ====================

    @Transactional
    public ProductionOrderResponse start(UUID productionOrderId) {
        UUID shopId = SecurityContextUtil.getCurrentShopId();
        UUID userId = SecurityContextUtil.getCurrentUserId();

        ProductionOrder order = productionOrderRepository.findByIdAndShopIdAndIsActiveTrue(productionOrderId, shopId)
                .orElseThrow(() -> new IllegalArgumentException("Production order not found"));

        if (order.getStatus() != ProductionStatus.PENDING) {
            throw new IllegalArgumentException("Only PENDING production orders can be started");
        }

        Recipe recipe = recipeRepository.findByIdAndShopIdAndIsActiveTrue(order.getRecipeId(), shopId)
                .orElseThrow(() -> new IllegalArgumentException("Recipe not found"));

        // Deduct ingredients from stock
        deductIngredients(recipe, order.getQuantityToProduce(), shopId);

        order.setStatus(ProductionStatus.IN_PROGRESS);
        order.setProducedBy(userId);
        order.setStartedAt(LocalDateTime.now());
        order.setIngredientsDeducted(true);

        ProductionOrder saved = productionOrderRepository.save(order);
        return buildResponse(saved, recipe);
    }

    // ==================== COMPLETE PRODUCTION (finished goods added to stock) ====================

    @Transactional
    public ProductionOrderResponse complete(ProductionCompleteRequest request) {
        UUID shopId = SecurityContextUtil.getCurrentShopId();

        ProductionOrder order = productionOrderRepository.findByIdAndShopIdAndIsActiveTrue(request.getProductionOrderId(), shopId)
                .orElseThrow(() -> new IllegalArgumentException("Production order not found"));

        if (order.getStatus() != ProductionStatus.IN_PROGRESS) {
            throw new IllegalArgumentException("Only IN_PROGRESS production orders can be completed");
        }

        if (request.getQuantityProduced().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Quantity produced must be greater than zero");
        }

        Recipe recipe = recipeRepository.findByIdAndShopIdAndIsActiveTrue(order.getRecipeId(), shopId)
                .orElseThrow(() -> new IllegalArgumentException("Recipe not found"));

        // Add finished goods to inventory stock
        addFinishedGoodsToStock(order.getVariantId(), request.getQuantityProduced(), shopId);

        // Calculate actual cost
        BigDecimal actualCost = calculateProductionCost(recipe, order.getQuantityToProduce());

        order.setQuantityProduced(request.getQuantityProduced());
        order.setActualCost(actualCost);
        order.setStatus(ProductionStatus.COMPLETED);
        order.setCompletedAt(LocalDateTime.now());
        order.setStockAdded(true);
        if (request.getNotes() != null) {
            order.setNotes(request.getNotes());
        }

        ProductionOrder saved = productionOrderRepository.save(order);
        return buildResponse(saved, recipe);
    }

    // ==================== CANCEL PRODUCTION ====================

    @Transactional
    public ProductionOrderResponse cancel(UUID productionOrderId, String reason) {
        UUID shopId = SecurityContextUtil.getCurrentShopId();

        ProductionOrder order = productionOrderRepository.findByIdAndShopIdAndIsActiveTrue(productionOrderId, shopId)
                .orElseThrow(() -> new IllegalArgumentException("Production order not found"));

        if (order.getStatus() == ProductionStatus.COMPLETED || order.getStatus() == ProductionStatus.CANCELLED) {
            throw new IllegalArgumentException("Cannot cancel a COMPLETED or already CANCELLED production order");
        }

        Recipe recipe = recipeRepository.findByIdAndShopIdAndIsActiveTrue(order.getRecipeId(), shopId)
                .orElse(null);

        // Restore ingredients if they were already deducted
        if (Boolean.TRUE.equals(order.getIngredientsDeducted()) && recipe != null) {
            restoreIngredients(recipe, order.getQuantityToProduce(), shopId);
            order.setIngredientsDeducted(false);
        }

        order.setStatus(ProductionStatus.CANCELLED);
        order.setCancelledAt(LocalDateTime.now());
        order.setCancelReason(reason);

        ProductionOrder saved = productionOrderRepository.save(order);
        return buildResponse(saved, recipe);
    }

    // ==================== QUERIES ====================

    public List<ProductionOrderResponse> fetch(ProductionFetchRequest request) {
        UUID shopId = SecurityContextUtil.getCurrentShopId();

        if (request.getId() != null) {
            ProductionOrder order = productionOrderRepository.findByIdAndShopIdAndIsActiveTrue(request.getId(), shopId)
                    .orElseThrow(() -> new IllegalArgumentException("Production order not found"));
            Recipe recipe = recipeRepository.findById(order.getRecipeId()).orElse(null);
            return List.of(buildResponse(order, recipe));
        }

        int start = request.getStart() != null ? request.getStart() : 0;
        int limit = request.getLimit() != null ? request.getLimit() : 50;

        List<ProductionOrder> orders = productionOrderRepository.searchFiltered(
                shopId,
                request.getSearch(),
                request.getStatus() != null ? request.getStatus().name() : null,
                request.getRecipeId(),
                start,
                limit
        );

        return orders.stream().map(o -> {
            Recipe recipe = recipeRepository.findById(o.getRecipeId()).orElse(null);
            return buildResponse(o, recipe);
        }).toList();
    }

    // ==================== PRIVATE HELPERS ====================

    private void deductIngredients(Recipe recipe, BigDecimal quantityToProduce, UUID shopId) {
        if (recipe.getYieldQuantity().compareTo(BigDecimal.ZERO) <= 0) return;

        BigDecimal ratio = quantityToProduce.divide(recipe.getYieldQuantity(), 6, RoundingMode.HALF_UP);

        for (RecipeIngredient ingredient : recipe.getIngredients()) {
            if (Boolean.TRUE.equals(ingredient.getIsOptional())) continue;

            BigDecimal wastePercent = ingredient.getWastePercentage() != null
                    ? ingredient.getWastePercentage() : BigDecimal.ZERO;
            BigDecimal wasteFactor = BigDecimal.ONE.add(wastePercent.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP));
            BigDecimal totalNeeded = ingredient.getQuantityRequired().multiply(ratio).multiply(wasteFactor)
                    .setScale(3, RoundingMode.HALF_UP);

            InventoryStock stock = inventoryStockRepository.findByVariantIdAndShopIdAndIsActiveTrue(
                    ingredient.getIngredientVariantId(), shopId).orElse(null);

            if (stock != null) {
                if (stock.getCurrentQuantity().compareTo(totalNeeded) < 0) {
                    throw new IllegalArgumentException(
                            "Insufficient stock for " + ingredient.getIngredientName() +
                                    ". Need " + totalNeeded + " " + ingredient.getUnitOfMeasure() +
                                    ", have " + stock.getCurrentQuantity());
                }
                stock.setCurrentQuantity(stock.getCurrentQuantity().subtract(totalNeeded));
                inventoryStockRepository.save(stock);
                stockAlertService.checkAndCreateAlert(shopId, ingredient.getIngredientVariantId());
            } else {
                throw new IllegalArgumentException("No stock record found for ingredient: " + ingredient.getIngredientName());
            }
        }
    }

    private void restoreIngredients(Recipe recipe, BigDecimal quantityToProduce, UUID shopId) {
        if (recipe.getYieldQuantity().compareTo(BigDecimal.ZERO) <= 0) return;

        BigDecimal ratio = quantityToProduce.divide(recipe.getYieldQuantity(), 6, RoundingMode.HALF_UP);

        for (RecipeIngredient ingredient : recipe.getIngredients()) {
            if (Boolean.TRUE.equals(ingredient.getIsOptional())) continue;

            BigDecimal wastePercent = ingredient.getWastePercentage() != null
                    ? ingredient.getWastePercentage() : BigDecimal.ZERO;
            BigDecimal wasteFactor = BigDecimal.ONE.add(wastePercent.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP));
            BigDecimal totalNeeded = ingredient.getQuantityRequired().multiply(ratio).multiply(wasteFactor)
                    .setScale(3, RoundingMode.HALF_UP);

            InventoryStock stock = inventoryStockRepository.findByVariantIdAndShopIdAndIsActiveTrue(
                    ingredient.getIngredientVariantId(), shopId).orElse(null);

            if (stock != null) {
                stock.setCurrentQuantity(stock.getCurrentQuantity().add(totalNeeded));
                inventoryStockRepository.save(stock);
                stockAlertService.checkAndResolveAlert(shopId, ingredient.getIngredientVariantId());
            }
        }
    }

    private void addFinishedGoodsToStock(UUID variantId, BigDecimal quantity, UUID shopId) {
        InventoryStock stock = inventoryStockRepository.findByVariantIdAndShopIdAndIsActiveTrue(variantId, shopId)
                .orElse(null);

        if (stock == null) {
            // Create stock record if it doesn't exist
            stock = InventoryStock.builder()
                    .shopId(shopId)
                    .variantId(variantId)
                    .currentQuantity(quantity)
                    .build();
        } else {
            stock.setCurrentQuantity(stock.getCurrentQuantity().add(quantity));
            stock.setLastRestockedAt(LocalDateTime.now());
        }

        inventoryStockRepository.save(stock);
        stockAlertService.checkAndResolveAlert(shopId, variantId);
    }

    private BigDecimal calculateProductionCost(Recipe recipe, BigDecimal quantityToProduce) {
        if (recipe.getYieldQuantity().compareTo(BigDecimal.ZERO) <= 0) return BigDecimal.ZERO;

        BigDecimal ratio = quantityToProduce.divide(recipe.getYieldQuantity(), 6, RoundingMode.HALF_UP);
        BigDecimal totalCost = BigDecimal.ZERO;

        for (RecipeIngredient ingredient : recipe.getIngredients()) {
            if (Boolean.TRUE.equals(ingredient.getIsOptional())) continue;

            BigDecimal wastePercent = ingredient.getWastePercentage() != null
                    ? ingredient.getWastePercentage() : BigDecimal.ZERO;
            BigDecimal wasteFactor = BigDecimal.ONE.add(wastePercent.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP));
            BigDecimal effectiveQty = ingredient.getQuantityRequired().multiply(ratio).multiply(wasteFactor);
            BigDecimal unitCost = ingredient.getUnitCost() != null ? ingredient.getUnitCost() : BigDecimal.ZERO;
            totalCost = totalCost.add(unitCost.multiply(effectiveQty));
        }

        return totalCost.setScale(2, RoundingMode.HALF_UP);
    }

    private String generateProductionNumber(UUID shopId) {
        List<String> existingNumbers = productionOrderRepository.findAllByShopIdAndIsActiveTrueOrderByCreatedAtDesc(shopId)
                .stream().map(ProductionOrder::getProductionNumber).toList();
        long count = productionOrderRepository.countByShopId(shopId);
        String code;
        do {
            count++;
            code = String.format("PROD-%04d", count);
        } while (existingNumbers.contains(code));
        return code;
    }

    // ==================== RESPONSE BUILDING ====================

    private ProductionOrderResponse buildResponse(ProductionOrder order, Recipe recipe) {
        List<ProductionOrderResponse.ProductionIngredient> ingredients = new ArrayList<>();

        if (recipe != null && recipe.getYieldQuantity().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal ratio = order.getQuantityToProduce().divide(recipe.getYieldQuantity(), 6, RoundingMode.HALF_UP);

            for (RecipeIngredient ing : recipe.getIngredients()) {
                BigDecimal wastePercent = ing.getWastePercentage() != null
                        ? ing.getWastePercentage() : BigDecimal.ZERO;
                BigDecimal wasteFactor = BigDecimal.ONE.add(wastePercent.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP));
                BigDecimal qtyUsed = ing.getQuantityRequired().multiply(ratio).multiply(wasteFactor)
                        .setScale(3, RoundingMode.HALF_UP);
                BigDecimal unitCost = ing.getUnitCost() != null ? ing.getUnitCost() : BigDecimal.ZERO;

                ingredients.add(ProductionOrderResponse.ProductionIngredient.builder()
                        .ingredientName(ing.getIngredientName())
                        .quantityUsed(qtyUsed)
                        .unitOfMeasure(ing.getUnitOfMeasure())
                        .unitCost(unitCost)
                        .totalCost(unitCost.multiply(qtyUsed).setScale(2, RoundingMode.HALF_UP))
                        .build());
            }
        }

        BigDecimal costPerUnit = null;
        BigDecimal yieldVariance = null;
        BigDecimal yieldPercentage = null;

        if (order.getQuantityProduced() != null && order.getQuantityToProduce().compareTo(BigDecimal.ZERO) > 0) {
            yieldVariance = order.getQuantityProduced().subtract(order.getQuantityToProduce());
            yieldPercentage = order.getQuantityProduced()
                    .divide(order.getQuantityToProduce(), 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100)).setScale(1, RoundingMode.HALF_UP);
        }

        if (order.getActualCost() != null && order.getQuantityProduced() != null
                && order.getQuantityProduced().compareTo(BigDecimal.ZERO) > 0) {
            costPerUnit = order.getActualCost().divide(order.getQuantityProduced(), 2, RoundingMode.HALF_UP);
        }

        ProductionOrderResponse response = ProductionOrderResponse.builder()
                .id(order.getId())
                .shopId(order.getShopId())
                .productionNumber(order.getProductionNumber())
                .recipeId(order.getRecipeId())
                .recipeName(order.getRecipeName())
                .productName(order.getProductName())
                .variantName(order.getVariantName())
                .variantId(order.getVariantId())
                .quantityToProduce(order.getQuantityToProduce())
                .quantityProduced(order.getQuantityProduced())
                .yieldUnit(order.getYieldUnit())
                .estimatedCost(order.getEstimatedCost())
                .actualCost(order.getActualCost())
                .costPerUnit(costPerUnit)
                .status(order.getStatus())
                .notes(order.getNotes())
                .requestedBy(order.getRequestedBy())
                .producedBy(order.getProducedBy())
                .startedAt(order.getStartedAt())
                .completedAt(order.getCompletedAt())
                .cancelledAt(order.getCancelledAt())
                .cancelReason(order.getCancelReason())
                .ingredientsDeducted(order.getIngredientsDeducted())
                .stockAdded(order.getStockAdded())
                .createdAt(order.getCreatedAt())
                .yieldVariance(yieldVariance)
                .yieldPercentage(yieldPercentage)
                .ingredients(ingredients.isEmpty() ? null : ingredients)
                .build();

        // Resolve names
        response.setRequestedByName(resolveName(order.getRequestedBy()));
        if (order.getProducedBy() != null) {
            response.setProducedByName(resolveName(order.getProducedBy()));
        }

        return response;
    }

    private String resolveName(UUID userId) {
        if (userId == null) return null;
        return userRepository.findById(userId)
                .map(User::getFullName).orElse(null);
    }
}
