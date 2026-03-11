package com.possystem.inventory;

import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProductVariantService {

    private final ProductVariantRepository productVariantRepository;
    private final ModelMapper modelMapper;

    ProductVariantResponse buildVariantResponse(ProductVariant variant) {
        return modelMapper.map(variant, ProductVariantResponse.class);
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
