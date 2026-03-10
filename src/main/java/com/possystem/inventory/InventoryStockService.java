package com.possystem.inventory;

import com.possystem.common.ListResponse;
import com.possystem.inventory.stockalert.StockAlertService;
import com.possystem.security.SecurityContextUtil;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
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
    private final StockAlertService stockAlertService;
    private final ModelMapper modelMapper;

    @Transactional
    public InventoryStockResponse save(InventoryStockRequest request) {
        UUID shopId = SecurityContextUtil.getCurrentShopId();

        if (request.getId() != null) {
            return updateStock(request, shopId);
        }
        return createStock(request, shopId);
    }

    public ListResponse<InventoryStockResponse> fetch(InventoryStockFetchRequest request) {
        UUID shopId = SecurityContextUtil.getCurrentShopId();

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
        UUID shopId = SecurityContextUtil.getCurrentShopId();
        InventoryStock stock = inventoryStockRepository.findByIdAndShopIdAndIsActiveTrue(id, shopId)
                .orElseThrow(() -> new IllegalArgumentException("Stock record not found"));
        stock.setIsActive(false);
        inventoryStockRepository.save(stock);
    }

    @Transactional
    public int bulkDelete(List<UUID> ids) {
        UUID shopId = SecurityContextUtil.getCurrentShopId();
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

        InventoryStock stock = modelMapper.map(request, InventoryStock.class);
        stock.setShopId(shopId);
        if (stock.getCurrentQuantity() == null) stock.setCurrentQuantity(BigDecimal.ZERO);

        InventoryStock saved = inventoryStockRepository.save(stock);
        return buildStockResponse(saved);
    }

    private InventoryStockResponse updateStock(InventoryStockRequest request, UUID shopId) {
        InventoryStock stock = inventoryStockRepository.findByIdAndShopIdAndIsActiveTrue(request.getId(), shopId)
                .orElseThrow(() -> new IllegalArgumentException("Stock record not found"));

        BigDecimal oldQuantity = stock.getCurrentQuantity();

        modelMapper.map(request, stock);

        // Track restock if quantity increased
        if (stock.getCurrentQuantity().compareTo(oldQuantity) > 0) {
            stock.setLastRestockedAt(LocalDateTime.now());
        }

        InventoryStock saved = inventoryStockRepository.save(stock);

        // Check stock alerts — resolve if restocked, create if reduced
        if (saved.getCurrentQuantity().compareTo(oldQuantity) > 0) {
            stockAlertService.checkAndResolveAlert(shopId, saved.getVariantId());
        } else if (saved.getCurrentQuantity().compareTo(oldQuantity) < 0) {
            stockAlertService.checkAndCreateAlert(shopId, saved.getVariantId());
        }

        return buildStockResponse(saved);
    }

    InventoryStockResponse buildStockResponse(InventoryStock stock) {
        InventoryStockResponse response = modelMapper.map(stock, InventoryStockResponse.class);

        // Resolve variant → product → category (computed fields)
        ProductVariant variant = productVariantRepository
                .findByIdAndShopIdAndIsActiveTrue(stock.getVariantId(), stock.getShopId())
                .orElse(null);

        if (variant != null) {
            response.setSku(variant.getSku());
            response.setCostPrice(variant.getCostPrice());
            response.setPrice(variant.getPrice());
            response.setVariantName(variant.getVariantName());

            Product product = productRepository
                    .findByIdAndShopIdAndIsActiveTrue(variant.getProductId(), stock.getShopId())
                    .orElse(null);

            if (product != null) {
                response.setProductId(product.getId());
                response.setProductName(product.getProductName());
                response.setProductStatus(product.getStatus() != null ? product.getStatus().name() : null);

                if (product.getCategoryId() != null) {
                    String categoryName = categoryRepository
                            .findByIdAndShopIdAndIsActiveTrue(product.getCategoryId(), stock.getShopId())
                            .map(Category::getCategoryName)
                            .orElse(null);
                    response.setCategoryName(categoryName);
                }
            }
        }

        // Compute stock status
        response.setStockStatus(computeStockStatus(stock));

        return response;
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

}
