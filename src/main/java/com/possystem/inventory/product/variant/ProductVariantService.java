package com.possystem.inventory;

import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProductVariantService {

    private final ProductVariantRepository productVariantRepository;
    private final InventoryStockRepository inventoryStockRepository;
    private final InventoryStockService inventoryStockService;
    private final ModelMapper modelMapper;

    ProductVariantResponse buildVariantResponse(ProductVariant variant) {
        ProductVariantResponse response = modelMapper.map(variant, ProductVariantResponse.class);

        // Resolve stock (computed field)
        InventoryStockResponse stockResponse = inventoryStockRepository
                .findByVariantIdAndShopIdAndIsActiveTrue(variant.getId(), variant.getShopId())
                .map(inventoryStockService::buildStockResponse)
                .orElse(null);
        response.setStock(stockResponse);

        return response;
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
