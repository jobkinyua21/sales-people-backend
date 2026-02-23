package com.possystem.inventory;

import com.possystem.common.ListResponse;
import com.possystem.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class InventoryStockService {

    private final InventoryStockRepository inventoryStockRepository;
    private final ProductVariantRepository productVariantRepository;
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    @Transactional
    public InventoryStockResponse save(InventoryStockRequest request) {
        UUID shopId = getCurrentShopId();

        if (request.getId() != null) {
            return updateStock(request, shopId);
        }
        return createStock(request, shopId);
    }

    public ListResponse<InventoryStockResponse> fetch(InventoryStockFetchRequest request) {
        UUID shopId = getCurrentShopId();

        if (request.getId() != null) {
            InventoryStock stock = inventoryStockRepository.findByIdAndShopIdAndIsActiveTrue(request.getId(), shopId)
                    .orElseThrow(() -> new IllegalArgumentException("Stock record not found"));
            List<InventoryStockResponse> result = List.of(buildStockResponse(stock));
            return ListResponse.of(result);
        }

        String search = request.getSearch();
        UUID categoryId = request.getCategoryId();
        String stockStatus = request.getStockStatus();
        Integer limit = request.getLimit();

        if (limit == null) {
            List<InventoryStock> all = inventoryStockRepository.searchFiltered(shopId, search, categoryId, stockStatus);
            List<InventoryStockResponse> responses = all.stream()
                    .map(this::buildStockResponse)
                    .toList();
            return ListResponse.of(responses);
        }

        PageRequest pageRequest = PageRequest.of(request.getStart(), limit);
        Page<InventoryStock> page = inventoryStockRepository.searchFiltered(shopId, search, categoryId, stockStatus, pageRequest);
        Page<InventoryStockResponse> responsePage = page.map(this::buildStockResponse);
        return ListResponse.from(responsePage);
    }

    @Transactional
    public void delete(UUID id) {
        UUID shopId = getCurrentShopId();
        InventoryStock stock = inventoryStockRepository.findByIdAndShopIdAndIsActiveTrue(id, shopId)
                .orElseThrow(() -> new IllegalArgumentException("Stock record not found"));
        stock.setIsActive(false);
        inventoryStockRepository.save(stock);
    }

    @Transactional
    public int bulkDelete(List<UUID> ids) {
        UUID shopId = getCurrentShopId();
        List<InventoryStock> stocks = inventoryStockRepository.findAllByIdInAndShopIdAndIsActiveTrue(ids, shopId);
        if (stocks.isEmpty()) {
            throw new IllegalArgumentException("No stock records found for the given IDs");
        }
        stocks.forEach(stock -> stock.setIsActive(false));
        inventoryStockRepository.saveAll(stocks);
        return stocks.size();
    }

    // ==================== INTERNAL ====================

    private InventoryStockResponse createStock(InventoryStockRequest request, UUID shopId) {
        productVariantRepository.findByIdAndShopIdAndIsActiveTrue(request.getVariantId(), shopId)
                .orElseThrow(() -> new IllegalArgumentException("Product variant not found"));

        if (inventoryStockRepository.findByVariantIdAndShopIdAndIsActiveTrue(request.getVariantId(), shopId).isPresent()) {
            throw new IllegalArgumentException("Stock record already exists for this variant");
        }

        InventoryStock stock = InventoryStock.builder()
                .shopId(shopId)
                .variantId(request.getVariantId())
                .currentQuantity(request.getCurrentQuantity() != null ? request.getCurrentQuantity() : BigDecimal.ZERO)
                .reorderLevel(request.getReorderLevel())
                .reorderQuantity(request.getReorderQuantity())
                .build();

        InventoryStock saved = inventoryStockRepository.save(stock);
        return buildStockResponse(saved);
    }

    private InventoryStockResponse updateStock(InventoryStockRequest request, UUID shopId) {
        InventoryStock stock = inventoryStockRepository.findByIdAndShopIdAndIsActiveTrue(request.getId(), shopId)
                .orElseThrow(() -> new IllegalArgumentException("Stock record not found"));

        BigDecimal oldQuantity = stock.getCurrentQuantity();

        if (request.getCurrentQuantity() != null) {
            stock.setCurrentQuantity(request.getCurrentQuantity());
        }
        if (request.getReorderLevel() != null) {
            stock.setReorderLevel(request.getReorderLevel());
        }
        if (request.getReorderQuantity() != null) {
            stock.setReorderQuantity(request.getReorderQuantity());
        }

        // Track restock if quantity increased
        if (stock.getCurrentQuantity().compareTo(oldQuantity) > 0) {
            stock.setLastRestockedAt(LocalDateTime.now());
        }

        InventoryStock saved = inventoryStockRepository.save(stock);
        return buildStockResponse(saved);
    }

    InventoryStockResponse buildStockResponse(InventoryStock stock) {
        // Resolve variant → product → category
        String productName = null;
        String sku = null;
        String categoryName = null;
        BigDecimal costPrice = null;
        BigDecimal price = null;
        String variantName = null;
        String productStatus = null;
        UUID productId = null;

        ProductVariant variant = productVariantRepository
                .findByIdAndShopIdAndIsActiveTrue(stock.getVariantId(), stock.getShopId())
                .orElse(null);

        if (variant != null) {
            sku = variant.getSku();
            costPrice = variant.getCostPrice();
            price = variant.getPrice();
            variantName = variant.getVariantName();

            Product product = productRepository
                    .findByIdAndShopIdAndIsActiveTrue(variant.getProductId(), stock.getShopId())
                    .orElse(null);

            if (product != null) {
                productId = product.getId();
                productName = product.getProductName();
                productStatus = product.getStatus() != null ? product.getStatus().name() : null;

                if (product.getCategoryId() != null) {
                    categoryName = categoryRepository
                            .findByIdAndShopIdAndIsActiveTrue(product.getCategoryId(), stock.getShopId())
                            .map(Category::getCategoryName)
                            .orElse(null);
                }
            }
        }

        // Compute stock status
        String stockStatus = computeStockStatus(stock);

        return InventoryStockResponse.builder()
                .id(stock.getId())
                .shopId(stock.getShopId())
                .variantId(stock.getVariantId())
                .productId(productId)
                .productName(productName)
                .sku(sku)
                .categoryName(categoryName)
                .costPrice(costPrice)
                .price(price)
                .variantName(variantName)
                .productStatus(productStatus)
                .currentQuantity(stock.getCurrentQuantity())
                .reorderLevel(stock.getReorderLevel())
                .reorderQuantity(stock.getReorderQuantity())
                .stockStatus(stockStatus)
                .lastRestockedAt(stock.getLastRestockedAt())
                .isActive(stock.getIsActive())
                .createdAt(stock.getCreatedAt())
                .updatedAt(stock.getUpdatedAt())
                .build();
    }

    private String computeStockStatus(InventoryStock stock) {
        BigDecimal qty = stock.getCurrentQuantity();
        if (qty == null || qty.compareTo(BigDecimal.ZERO) <= 0) {
            return "OUT_OF_STOCK";
        }
        if (stock.getReorderLevel() != null && qty.compareTo(stock.getReorderLevel()) <= 0) {
            return "LOW_STOCK";
        }
        return "IN_STOCK";
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
}
