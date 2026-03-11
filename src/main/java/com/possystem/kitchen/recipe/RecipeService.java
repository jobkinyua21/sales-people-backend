package com.possystem.kitchen.recipe;

import com.possystem.auth.user.User;
import com.possystem.auth.user.UserRepository;
import com.possystem.inventory.InventoryStock;
import com.possystem.inventory.InventoryStockRepository;
import com.possystem.inventory.ProductVariant;
import com.possystem.inventory.ProductVariantRepository;
import com.possystem.inventory.Product;
import com.possystem.inventory.ProductRepository;
import com.possystem.inventory.stockalert.StockAlertService;
import com.possystem.security.SecurityContextUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RecipeService {

    private final RecipeRepository recipeRepository;
    private final RecipeIngredientRepository recipeIngredientRepository;
    private final ProductVariantRepository productVariantRepository;
    private final ProductRepository productRepository;
    private final InventoryStockRepository inventoryStockRepository;
    private final StockAlertService stockAlertService;
    private final UserRepository userRepository;

    // ==================== SAVE (CREATE/UPDATE) ====================

    @Transactional
    public RecipeResponse save(RecipeRequest request) {
        UUID shopId = SecurityContextUtil.getCurrentShopId();

        // Validate finished product variant exists
        ProductVariant finishedVariant = productVariantRepository.findByIdAndShopIdAndIsActiveTrue(request.getVariantId(), shopId)
                .orElseThrow(() -> new IllegalArgumentException("Product variant not found"));

        Recipe recipe;
        if (request.getId() != null) {
            recipe = recipeRepository.findByIdAndShopIdAndIsActiveTrue(request.getId(), shopId)
                    .orElseThrow(() -> new IllegalArgumentException("Recipe not found"));

            // Clear existing ingredients for rebuild
            recipe.getIngredients().clear();
        } else {
            recipe = Recipe.builder()
                    .shopId(shopId)
                    .build();
        }

        recipe.setVariantId(request.getVariantId());
        recipe.setRecipeName(request.getRecipeName());
        recipe.setDescription(request.getDescription());
        recipe.setYieldQuantity(request.getYieldQuantity());
        recipe.setYieldUnit(request.getYieldUnit());
        recipe.setPrepTimeMinutes(request.getPrepTimeMinutes());
        recipe.setCookTimeMinutes(request.getCookTimeMinutes());
        recipe.setPrepStation(request.getPrepStation());
        recipe.setProductionType(request.getProductionType() != null ? request.getProductionType() : ProductionType.COOK_TO_ORDER);
        recipe.setInstructions(request.getInstructions());

        // Build ingredients
        BigDecimal totalRecipeCost = BigDecimal.ZERO;
        List<RecipeIngredient> ingredients = new ArrayList<>();

        for (RecipeIngredientRequest ingredientReq : request.getIngredients()) {
            ProductVariant ingredientVariant = productVariantRepository.findByIdAndShopIdAndIsActiveTrue(
                    ingredientReq.getIngredientVariantId(), shopId)
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Ingredient variant not found: " + ingredientReq.getIngredientVariantId()));

            // Get product name for snapshot
            Product product = productRepository.findById(ingredientVariant.getProductId()).orElse(null);
            String productName = product != null ? product.getProductName() : ingredientVariant.getVariantName();

            BigDecimal unitCost = ingredientVariant.getCostPrice() != null
                    ? ingredientVariant.getCostPrice() : BigDecimal.ZERO;

            // Calculate effective quantity accounting for waste
            BigDecimal wastePercent = ingredientReq.getWastePercentage() != null
                    ? ingredientReq.getWastePercentage() : BigDecimal.ZERO;
            BigDecimal wasteFactor = BigDecimal.ONE.add(wastePercent.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP));
            BigDecimal effectiveQty = ingredientReq.getQuantityRequired().multiply(wasteFactor);

            BigDecimal ingredientCost = unitCost.multiply(effectiveQty);
            totalRecipeCost = totalRecipeCost.add(ingredientCost);

            RecipeIngredient ingredient = RecipeIngredient.builder()
                    .recipe(recipe)
                    .ingredientVariantId(ingredientReq.getIngredientVariantId())
                    .ingredientName(productName)
                    .variantName(ingredientVariant.getVariantName())
                    .sku(ingredientVariant.getSku())
                    .quantityRequired(ingredientReq.getQuantityRequired())
                    .unitOfMeasure(ingredientReq.getUnitOfMeasure() != null
                            ? ingredientReq.getUnitOfMeasure() : ingredientVariant.getUom())
                    .unitCost(unitCost)
                    .wastePercentage(wastePercent)
                    .isOptional(ingredientReq.getIsOptional() != null ? ingredientReq.getIsOptional() : false)
                    .notes(ingredientReq.getNotes())
                    .build();

            ingredients.add(ingredient);
        }

        recipe.getIngredients().addAll(ingredients);

        // Calculate cost per unit of finished product
        if (request.getYieldQuantity().compareTo(BigDecimal.ZERO) > 0) {
            recipe.setCostPerUnit(totalRecipeCost.divide(request.getYieldQuantity(), 2, RoundingMode.HALF_UP));
        }

        Recipe saved = recipeRepository.save(recipe);
        return buildResponse(saved, shopId);
    }

    // ==================== ACTIVATE / DEACTIVATE ====================

    @Transactional
    public RecipeResponse activate(UUID recipeId) {
        UUID shopId = SecurityContextUtil.getCurrentShopId();
        Recipe recipe = recipeRepository.findByIdAndShopIdAndIsActiveTrue(recipeId, shopId)
                .orElseThrow(() -> new IllegalArgumentException("Recipe not found"));

        recipe.setStatus(RecipeStatus.ACTIVE);
        Recipe saved = recipeRepository.save(recipe);
        return buildResponse(saved, shopId);
    }

    @Transactional
    public RecipeResponse deactivate(UUID recipeId) {
        UUID shopId = SecurityContextUtil.getCurrentShopId();
        Recipe recipe = recipeRepository.findByIdAndShopIdAndIsActiveTrue(recipeId, shopId)
                .orElseThrow(() -> new IllegalArgumentException("Recipe not found"));

        recipe.setStatus(RecipeStatus.INACTIVE);
        Recipe saved = recipeRepository.save(recipe);
        return buildResponse(saved, shopId);
    }

    // ==================== FETCH ====================

    public List<RecipeResponse> fetch(RecipeFetchRequest request) {
        UUID shopId = SecurityContextUtil.getCurrentShopId();

        if (request.getId() != null) {
            Recipe recipe = recipeRepository.findByIdAndShopIdAndIsActiveTrue(request.getId(), shopId)
                    .orElseThrow(() -> new IllegalArgumentException("Recipe not found"));
            return List.of(buildResponse(recipe, shopId));
        }

        int start = request.getStart() != null ? request.getStart() : 0;
        int limit = request.getLimit() != null ? request.getLimit() : 50;

        List<Recipe> recipes = recipeRepository.searchFiltered(
                shopId,
                request.getSearch(),
                request.getStatus() != null ? request.getStatus().name() : null,
                request.getPrepStation() != null ? request.getPrepStation().name() : null,
                request.getVariantId(),
                start,
                limit
        );

        return recipes.stream().map(r -> buildResponse(r, shopId)).toList();
    }

    // ==================== INGREDIENT DEDUCTION ====================

    /**
     * Deducts ingredients from inventory for preparing a given quantity of the finished product.
     * Called by KitchenOrderTicketService when a KOT item starts preparation.
     *
     * @param recipeId The recipe to use
     * @param quantity How many units of the finished product to prepare
     * @param shopId The shop
     */
    @Transactional
    public void deductIngredients(UUID recipeId, BigDecimal quantity, UUID shopId) {
        Recipe recipe = recipeRepository.findByIdAndShopIdAndIsActiveTrue(recipeId, shopId)
                .orElseThrow(() -> new IllegalArgumentException("Recipe not found"));

        if (recipe.getYieldQuantity().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Recipe yield quantity must be greater than zero");
        }

        // Calculate the ratio: how much of the recipe to use
        // e.g., recipe yields 20 chapatis, we need 5 → ratio = 5/20 = 0.25
        BigDecimal ratio = quantity.divide(recipe.getYieldQuantity(), 6, RoundingMode.HALF_UP);

        for (RecipeIngredient ingredient : recipe.getIngredients()) {
            if (Boolean.TRUE.equals(ingredient.getIsOptional())) {
                continue; // Skip optional ingredients
            }

            // Calculate actual quantity needed (including waste)
            BigDecimal wastePercent = ingredient.getWastePercentage() != null
                    ? ingredient.getWastePercentage() : BigDecimal.ZERO;
            BigDecimal wasteFactor = BigDecimal.ONE.add(wastePercent.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP));
            BigDecimal baseNeeded = ingredient.getQuantityRequired().multiply(ratio);
            BigDecimal totalNeeded = baseNeeded.multiply(wasteFactor).setScale(3, RoundingMode.HALF_UP);

            // Deduct from inventory
            InventoryStock stock = inventoryStockRepository.findByVariantIdAndShopIdAndIsActiveTrue(
                    ingredient.getIngredientVariantId(), shopId).orElse(null);

            if (stock != null) {
                stock.setCurrentQuantity(stock.getCurrentQuantity().subtract(totalNeeded));
                inventoryStockRepository.save(stock);

                // Check stock alerts
                stockAlertService.checkAndCreateAlert(shopId, ingredient.getIngredientVariantId());
            }
        }
    }

    /**
     * Restores ingredients to inventory when a KOT item is cancelled.
     */
    @Transactional
    public void restoreIngredients(UUID recipeId, BigDecimal quantity, UUID shopId) {
        Recipe recipe = recipeRepository.findByIdAndShopIdAndIsActiveTrue(recipeId, shopId)
                .orElseThrow(() -> new IllegalArgumentException("Recipe not found"));

        if (recipe.getYieldQuantity().compareTo(BigDecimal.ZERO) <= 0) return;

        BigDecimal ratio = quantity.divide(recipe.getYieldQuantity(), 6, RoundingMode.HALF_UP);

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

    /**
     * Checks if there are enough ingredients in stock to prepare a given quantity.
     * Returns a list of insufficient ingredients (empty = all good).
     */
    public List<String> checkIngredientAvailability(UUID recipeId, BigDecimal quantity, UUID shopId) {
        Recipe recipe = recipeRepository.findByIdAndShopIdAndIsActiveTrue(recipeId, shopId)
                .orElseThrow(() -> new IllegalArgumentException("Recipe not found"));

        List<String> insufficientIngredients = new ArrayList<>();

        if (recipe.getYieldQuantity().compareTo(BigDecimal.ZERO) <= 0) return insufficientIngredients;

        BigDecimal ratio = quantity.divide(recipe.getYieldQuantity(), 6, RoundingMode.HALF_UP);

        for (RecipeIngredient ingredient : recipe.getIngredients()) {
            if (Boolean.TRUE.equals(ingredient.getIsOptional())) continue;

            BigDecimal wastePercent = ingredient.getWastePercentage() != null
                    ? ingredient.getWastePercentage() : BigDecimal.ZERO;
            BigDecimal wasteFactor = BigDecimal.ONE.add(wastePercent.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP));
            BigDecimal totalNeeded = ingredient.getQuantityRequired().multiply(ratio).multiply(wasteFactor)
                    .setScale(3, RoundingMode.HALF_UP);

            InventoryStock stock = inventoryStockRepository.findByVariantIdAndShopIdAndIsActiveTrue(
                    ingredient.getIngredientVariantId(), shopId).orElse(null);

            BigDecimal available = stock != null ? stock.getCurrentQuantity() : BigDecimal.ZERO;
            if (available.compareTo(totalNeeded) < 0) {
                insufficientIngredients.add(ingredient.getIngredientName() +
                        " (need " + totalNeeded + " " + ingredient.getUnitOfMeasure() +
                        ", have " + available + ")");
            }
        }

        return insufficientIngredients;
    }

    // ==================== PREP SHEET (Chef's View) ====================

    /**
     * Returns a prep sheet for a specific recipe scaled to the requested quantity.
     * Shows exactly what ingredients the chef needs, current stock levels, and availability.
     */
    public PrepSheetResponse getPrepSheet(UUID recipeId, BigDecimal quantity) {
        UUID shopId = SecurityContextUtil.getCurrentShopId();

        Recipe recipe = recipeRepository.findByIdAndShopIdAndIsActiveTrue(recipeId, shopId)
                .orElseThrow(() -> new IllegalArgumentException("Recipe not found"));

        // Get product name
        ProductVariant finishedVariant = productVariantRepository.findByIdAndShopIdAndIsActiveTrue(
                recipe.getVariantId(), shopId).orElse(null);
        Product product = finishedVariant != null
                ? productRepository.findById(finishedVariant.getProductId()).orElse(null) : null;

        BigDecimal ratio = BigDecimal.ONE;
        if (recipe.getYieldQuantity().compareTo(BigDecimal.ZERO) > 0) {
            ratio = quantity.divide(recipe.getYieldQuantity(), 6, RoundingMode.HALF_UP);
        }

        List<PrepSheetResponse.PrepIngredient> ingredients = new ArrayList<>();
        List<String> insufficientList = new ArrayList<>();
        boolean allAvailable = true;

        for (RecipeIngredient ingredient : recipe.getIngredients()) {
            BigDecimal wastePercent = ingredient.getWastePercentage() != null
                    ? ingredient.getWastePercentage() : BigDecimal.ZERO;

            BigDecimal baseNeeded = ingredient.getQuantityRequired().multiply(ratio)
                    .setScale(3, RoundingMode.HALF_UP);
            BigDecimal wasteAllowance = baseNeeded.multiply(wastePercent)
                    .divide(BigDecimal.valueOf(100), 3, RoundingMode.HALF_UP);
            BigDecimal totalNeeded = baseNeeded.add(wasteAllowance);

            InventoryStock stock = inventoryStockRepository.findByVariantIdAndShopIdAndIsActiveTrue(
                    ingredient.getIngredientVariantId(), shopId).orElse(null);
            BigDecimal currentStock = stock != null ? stock.getCurrentQuantity() : BigDecimal.ZERO;

            boolean sufficient = currentStock.compareTo(totalNeeded) >= 0
                    || Boolean.TRUE.equals(ingredient.getIsOptional());

            if (!sufficient) {
                allAvailable = false;
                insufficientList.add(ingredient.getIngredientName() +
                        " (need " + totalNeeded + " " + ingredient.getUnitOfMeasure() +
                        ", have " + currentStock + ")");
            }

            ingredients.add(PrepSheetResponse.PrepIngredient.builder()
                    .ingredientVariantId(ingredient.getIngredientVariantId())
                    .ingredientName(ingredient.getIngredientName())
                    .variantName(ingredient.getVariantName())
                    .quantityNeeded(baseNeeded)
                    .wasteAllowance(wasteAllowance)
                    .totalQuantityNeeded(totalNeeded)
                    .unitOfMeasure(ingredient.getUnitOfMeasure())
                    .currentStock(currentStock)
                    .sufficient(sufficient)
                    .optional(Boolean.TRUE.equals(ingredient.getIsOptional()))
                    .notes(ingredient.getNotes())
                    .build());
        }

        return PrepSheetResponse.builder()
                .recipeId(recipe.getId())
                .recipeName(recipe.getRecipeName())
                .productName(product != null ? product.getProductName() : null)
                .variantName(finishedVariant != null ? finishedVariant.getVariantName() : null)
                .quantityToPrepare(quantity)
                .recipeYield(recipe.getYieldQuantity())
                .yieldUnit(recipe.getYieldUnit())
                .estimatedPrepMinutes(recipe.getPrepTimeMinutes())
                .estimatedCookMinutes(recipe.getCookTimeMinutes())
                .prepStation(recipe.getPrepStation())
                .instructions(recipe.getInstructions())
                .ingredients(ingredients)
                .allIngredientsAvailable(allAvailable)
                .insufficientIngredients(insufficientList.isEmpty() ? null : insufficientList)
                .build();
    }

    // ==================== DELETE ====================

    @Transactional
    public void delete(UUID recipeId) {
        UUID shopId = SecurityContextUtil.getCurrentShopId();
        Recipe recipe = recipeRepository.findByIdAndShopIdAndIsActiveTrue(recipeId, shopId)
                .orElseThrow(() -> new IllegalArgumentException("Recipe not found"));

        recipe.setIsActive(false);
        recipeRepository.save(recipe);
    }

    // ==================== RESPONSE BUILDING ====================

    private RecipeResponse buildResponse(Recipe recipe, UUID shopId) {
        // Get product name for the finished product
        ProductVariant finishedVariant = productVariantRepository.findByIdAndShopIdAndIsActiveTrue(
                recipe.getVariantId(), shopId).orElse(null);
        Product product = finishedVariant != null
                ? productRepository.findById(finishedVariant.getProductId()).orElse(null) : null;

        BigDecimal totalRecipeCost = BigDecimal.ZERO;
        List<RecipeResponse.RecipeIngredientResponse> ingredientResponses = new ArrayList<>();

        for (RecipeIngredient ingredient : recipe.getIngredients()) {
            BigDecimal wastePercent = ingredient.getWastePercentage() != null
                    ? ingredient.getWastePercentage() : BigDecimal.ZERO;
            BigDecimal wasteFactor = BigDecimal.ONE.add(wastePercent.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP));
            BigDecimal effectiveQty = ingredient.getQuantityRequired().multiply(wasteFactor)
                    .setScale(3, RoundingMode.HALF_UP);

            BigDecimal unitCost = ingredient.getUnitCost() != null ? ingredient.getUnitCost() : BigDecimal.ZERO;
            BigDecimal totalCost = unitCost.multiply(effectiveQty).setScale(2, RoundingMode.HALF_UP);
            totalRecipeCost = totalRecipeCost.add(totalCost);

            // Get current stock level
            InventoryStock stock = inventoryStockRepository.findByVariantIdAndShopIdAndIsActiveTrue(
                    ingredient.getIngredientVariantId(), shopId).orElse(null);

            ingredientResponses.add(RecipeResponse.RecipeIngredientResponse.builder()
                    .id(ingredient.getId())
                    .ingredientVariantId(ingredient.getIngredientVariantId())
                    .ingredientName(ingredient.getIngredientName())
                    .variantName(ingredient.getVariantName())
                    .sku(ingredient.getSku())
                    .quantityRequired(ingredient.getQuantityRequired())
                    .unitOfMeasure(ingredient.getUnitOfMeasure())
                    .unitCost(unitCost)
                    .totalCost(totalCost)
                    .wastePercentage(wastePercent)
                    .effectiveQuantity(effectiveQty)
                    .isOptional(ingredient.getIsOptional())
                    .notes(ingredient.getNotes())
                    .currentStock(stock != null ? stock.getCurrentQuantity() : BigDecimal.ZERO)
                    .build());
        }

        BigDecimal costPerUnit = recipe.getYieldQuantity().compareTo(BigDecimal.ZERO) > 0
                ? totalRecipeCost.divide(recipe.getYieldQuantity(), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        return RecipeResponse.builder()
                .id(recipe.getId())
                .shopId(recipe.getShopId())
                .variantId(recipe.getVariantId())
                .productName(product != null ? product.getProductName() : null)
                .variantName(finishedVariant != null ? finishedVariant.getVariantName() : null)
                .recipeName(recipe.getRecipeName())
                .description(recipe.getDescription())
                .yieldQuantity(recipe.getYieldQuantity())
                .yieldUnit(recipe.getYieldUnit())
                .prepTimeMinutes(recipe.getPrepTimeMinutes())
                .cookTimeMinutes(recipe.getCookTimeMinutes())
                .prepStation(recipe.getPrepStation())
                .productionType(recipe.getProductionType())
                .status(recipe.getStatus())
                .costPerUnit(costPerUnit)
                .totalRecipeCost(totalRecipeCost)
                .instructions(recipe.getInstructions())
                .ingredientCostPerUnit(costPerUnit)
                .createdAt(recipe.getCreatedAt())
                .ingredients(ingredientResponses)
                .build();
    }
}
