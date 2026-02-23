package com.possystem.inventory;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProductVariantService {

    private final ProductVariantRepository productVariantRepository;
    private final InventoryStockRepository inventoryStockRepository;
    private final InventoryStockService inventoryStockService;

    ProductVariantResponse buildVariantResponse(ProductVariant variant) {
        InventoryStockResponse stockResponse = inventoryStockRepository
                .findByVariantIdAndShopIdAndIsActiveTrue(variant.getId(), variant.getShopId())
                .map(inventoryStockService::buildStockResponse)
                .orElse(null);

        return ProductVariantResponse.builder()
                .id(variant.getId())
                .shopId(variant.getShopId())
                .productId(variant.getProductId())
                .sku(variant.getSku())
                .variantName(variant.getVariantName())
                .price(variant.getPrice())
                .costPrice(variant.getCostPrice())
                .compareAtPrice(variant.getCompareAtPrice())
                .barcode(variant.getBarcode())
                .weight(variant.getWeight())
                .uom(variant.getUom())
                .trackStock(variant.getTrackStock())
                .isDefault(variant.getIsDefault())
                .sortOrder(variant.getSortOrder())
                .status(variant.getStatus())
                .isActive(variant.getIsActive())
                .createdAt(variant.getCreatedAt())
                .updatedAt(variant.getUpdatedAt())
                .stock(stockResponse)
                .build();
    }

    String generateSku(UUID shopId) {
        long count = productVariantRepository.countByShopId(shopId);
        String sku;
        do {
            count++;
            sku = String.format("SKU-%04d", count);
        } while (productVariantRepository.existsByShopIdAndSku(shopId, sku));
        return sku;
    }
}
