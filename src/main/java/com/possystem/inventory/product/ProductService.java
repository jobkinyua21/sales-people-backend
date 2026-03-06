package com.possystem.inventory;

import com.possystem.common.FetchRequest;
import com.possystem.common.ListResponse;
import com.possystem.security.SecurityContextUtil;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;
    private final InventoryStockRepository inventoryStockRepository;
    private final CategoryRepository categoryRepository;
    private final ProductVariantService productVariantService;
    private final ModelMapper modelMapper;

    // ==================== CRUD ====================

    @Transactional
    public ProductResponse save(ProductRequest request) {
        UUID shopId = SecurityContextUtil.getCurrentShopId();

        if (request.getId() != null) {
            return updateProduct(request, shopId);
        }
        return createProduct(request, shopId);
    }

    public ListResponse<ProductResponse> fetch(FetchRequest request) {
        UUID shopId = SecurityContextUtil.getCurrentShopId();

        if (request.getId() != null) {
            Product product = productRepository.findByIdAndShopIdAndIsActiveTrue(request.getId(), shopId)
                    .orElseThrow(() -> new IllegalArgumentException("Product not found"));
            List<ProductResponse> result = List.of(buildProductResponse(product));
            return ListResponse.of(result);
        }

        String search = request.getSearch();
        Integer limit = request.getLimit();

        if (limit == null) {
            List<Product> all = productRepository.searchAll(shopId, search);
            List<ProductResponse> responses = all.stream()
                    .map(this::buildProductResponse)
                    .toList();
            return ListResponse.of(responses);
        }

        PageRequest pageRequest = PageRequest.of(request.getStart(), limit);
        Page<Product> page = productRepository.searchAll(shopId, search, pageRequest);
        Page<ProductResponse> responsePage = page.map(this::buildProductResponse);
        return ListResponse.from(responsePage);
    }

    @Transactional
    public void delete(UUID id) {
        UUID shopId = SecurityContextUtil.getCurrentShopId();
        Product product = productRepository.findByIdAndShopIdAndIsActiveTrue(id, shopId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found"));

        product.setIsActive(false);
        product.setStatus(ProductStatus.INACTIVE);
        productRepository.save(product);

        softDeleteVariantsAndStock(product.getId(), shopId);
    }

    @Transactional
    public int bulkDelete(List<UUID> ids) {
        UUID shopId = SecurityContextUtil.getCurrentShopId();
        List<Product> products = productRepository.findAllByIdInAndShopIdAndIsActiveTrue(ids, shopId);
        if (products.isEmpty()) {
            throw new IllegalArgumentException("No products found for the given IDs");
        }
        for (Product product : products) {
            product.setIsActive(false);
            product.setStatus(ProductStatus.INACTIVE);
            productRepository.save(product);
            softDeleteVariantsAndStock(product.getId(), shopId);
        }
        return products.size();
    }

    private void softDeleteVariantsAndStock(UUID productId, UUID shopId) {
        List<ProductVariant> variants = productVariantRepository
                .findByProductIdAndShopIdAndIsActiveTrueOrderBySortOrderAsc(productId, shopId);
        for (ProductVariant variant : variants) {
            variant.setIsActive(false);
            variant.setStatus(ProductVariantStatus.INACTIVE);
            productVariantRepository.save(variant);

            inventoryStockRepository.findByVariantIdAndShopIdAndIsActiveTrue(variant.getId(), shopId)
                    .ifPresent(stock -> {
                        stock.setIsActive(false);
                        inventoryStockRepository.save(stock);
                    });
        }
    }

    // ==================== CREATE / UPDATE ====================

    private ProductResponse createProduct(ProductRequest request, UUID shopId) {
        if (productRepository.existsByShopIdAndProductNameIgnoreCaseAndIsActiveTrue(shopId, request.getProductName())) {
            throw new IllegalArgumentException("A product with this name already exists");
        }

        if (request.getCategoryId() != null) {
            categoryRepository.findByIdAndShopIdAndIsActiveTrue(request.getCategoryId(), shopId)
                    .orElseThrow(() -> new IllegalArgumentException("Category not found"));
        }

        ProductType productType = request.getProductType() != null ? request.getProductType() : ProductType.SIMPLE;

        Product product = modelMapper.map(request, Product.class);
        product.setShopId(shopId);
        product.setProductCode(generateProductCode(shopId));
        product.setProductType(productType);
        if (product.getStatus() == null) product.setStatus(ProductStatus.ACTIVE);

        Product saved = productRepository.save(product);

        if (productType == ProductType.SIMPLE) {
            createDefaultVariant(saved, request.getDefaultVariant(), shopId);
        }

        if (productType == ProductType.VARIABLE && request.getVariants() != null) {
            for (ProductVariantRequest variantReq : request.getVariants()) {
                createVariantFromRequest(saved, variantReq, shopId);
            }
        }

        return buildProductResponse(saved);
    }

    private ProductResponse updateProduct(ProductRequest request, UUID shopId) {
        Product product = productRepository.findByIdAndShopIdAndIsActiveTrue(request.getId(), shopId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found"));

        if (request.getProductName() != null && !product.getProductName().equalsIgnoreCase(request.getProductName())) {
            if (productRepository.existsByShopIdAndProductNameIgnoreCaseAndIsActiveTrueAndIdNot(
                    shopId, request.getProductName(), product.getId())) {
                throw new IllegalArgumentException("A product with this name already exists");
            }
        }

        if (request.getCategoryId() != null) {
            categoryRepository.findByIdAndShopIdAndIsActiveTrue(request.getCategoryId(), shopId)
                    .orElseThrow(() -> new IllegalArgumentException("Category not found"));
        }

        modelMapper.map(request, product);

        Product saved = productRepository.save(product);

        if (product.getProductType() == ProductType.SIMPLE && request.getDefaultVariant() != null) {
            updateDefaultVariant(saved, request.getDefaultVariant(), shopId);
        }

        if (product.getProductType() == ProductType.VARIABLE && request.getVariants() != null) {
            syncVariants(saved, request.getVariants(), shopId);
        }

        return buildProductResponse(saved);
    }

    // ==================== HELPERS ====================

    private void createDefaultVariant(Product product, ProductVariantRequest variantReq, UUID shopId) {
        BigDecimal price = BigDecimal.ZERO;
        BigDecimal costPrice = null;
        BigDecimal compareAtPrice = null;
        String barcode = null;
        BigDecimal weight = null;
        String uom = null;
        boolean trackStock = true;

        if (variantReq != null) {
            if (variantReq.getPrice() != null) price = variantReq.getPrice();
            costPrice = variantReq.getCostPrice();
            compareAtPrice = variantReq.getCompareAtPrice();
            barcode = variantReq.getBarcode();
            weight = variantReq.getWeight();
            uom = variantReq.getUom();
            if (variantReq.getTrackStock() != null) trackStock = variantReq.getTrackStock();
        }

        ProductVariant variant = ProductVariant.builder()
                .shopId(shopId)
                .productId(product.getId())
                .sku(productVariantService.generateSku(shopId))
                .variantName(product.getProductName())
                .price(price)
                .costPrice(costPrice)
                .compareAtPrice(compareAtPrice)
                .barcode(barcode)
                .weight(weight)
                .uom(uom)
                .trackStock(trackStock)
                .isDefault(true)
                .build();

        ProductVariant saved = productVariantRepository.save(variant);

        if (trackStock) {
            InventoryStock stock = InventoryStock.builder()
                    .shopId(shopId)
                    .variantId(saved.getId())
                    .currentQuantity(BigDecimal.ZERO)
                    .build();
            inventoryStockRepository.save(stock);
        }
    }

    private void updateDefaultVariant(Product product, ProductVariantRequest variantReq, UUID shopId) {
        ProductVariant defaultVariant = productVariantRepository
                .findByProductIdAndShopIdAndIsDefaultTrueAndIsActiveTrue(product.getId(), shopId)
                .orElse(null);

        if (defaultVariant == null) return;

        defaultVariant.setVariantName(product.getProductName());
        modelMapper.map(variantReq, defaultVariant);

        productVariantRepository.save(defaultVariant);
    }

    private void createVariantFromRequest(Product product, ProductVariantRequest variantReq, UUID shopId) {
        String sku = variantReq.getSku();
        if (sku == null || sku.isBlank()) {
            sku = productVariantService.generateSku(shopId);
        } else if (productVariantRepository.existsByShopIdAndSku(shopId, sku)) {
            throw new IllegalArgumentException("A variant with SKU '" + sku + "' already exists");
        }

        boolean trackStock = variantReq.getTrackStock() != null ? variantReq.getTrackStock() : true;

        ProductVariant variant = ProductVariant.builder()
                .shopId(shopId)
                .productId(product.getId())
                .sku(sku)
                .variantName(variantReq.getVariantName() != null ? variantReq.getVariantName() : product.getProductName())
                .price(variantReq.getPrice() != null ? variantReq.getPrice() : BigDecimal.ZERO)
                .costPrice(variantReq.getCostPrice())
                .compareAtPrice(variantReq.getCompareAtPrice())
                .barcode(variantReq.getBarcode())
                .weight(variantReq.getWeight())
                .uom(variantReq.getUom())
                .trackStock(trackStock)
                .sortOrder(variantReq.getSortOrder() != null ? variantReq.getSortOrder() : 0)
                .status(variantReq.getStatus() != null ? variantReq.getStatus() : ProductVariantStatus.ACTIVE)
                .build();

        ProductVariant saved = productVariantRepository.save(variant);

        if (trackStock) {
            InventoryStock stock = InventoryStock.builder()
                    .shopId(shopId)
                    .variantId(saved.getId())
                    .currentQuantity(BigDecimal.ZERO)
                    .build();
            inventoryStockRepository.save(stock);
        }
    }

    private void syncVariants(Product product, List<ProductVariantRequest> variantRequests, UUID shopId) {
        List<ProductVariant> existingVariants = productVariantRepository
                .findByProductIdAndShopIdAndIsActiveTrueOrderBySortOrderAsc(product.getId(), shopId);

        Set<UUID> requestVariantIds = new HashSet<>();
        for (ProductVariantRequest req : variantRequests) {
            if (req.getId() != null) {
                requestVariantIds.add(req.getId());
            }
        }

        for (ProductVariant existing : existingVariants) {
            if (!requestVariantIds.contains(existing.getId())) {
                existing.setIsActive(false);
                existing.setStatus(ProductVariantStatus.INACTIVE);
                productVariantRepository.save(existing);

                inventoryStockRepository.findByVariantIdAndShopIdAndIsActiveTrue(existing.getId(), shopId)
                        .ifPresent(stock -> {
                            stock.setIsActive(false);
                            inventoryStockRepository.save(stock);
                        });
            }
        }

        for (ProductVariantRequest req : variantRequests) {
            if (req.getId() != null) {
                ProductVariant variant = productVariantRepository.findByIdAndShopIdAndIsActiveTrue(req.getId(), shopId)
                        .orElseThrow(() -> new IllegalArgumentException("Variant not found: " + req.getId()));

                if (req.getSku() != null && !req.getSku().isBlank() && !req.getSku().equals(variant.getSku())) {
                    if (productVariantRepository.existsByShopIdAndSkuAndIsActiveTrueAndIdNot(shopId, req.getSku(), variant.getId())) {
                        throw new IllegalArgumentException("A variant with SKU '" + req.getSku() + "' already exists");
                    }
                }

                modelMapper.map(req, variant);

                productVariantRepository.save(variant);
            } else {
                createVariantFromRequest(product, req, shopId);
            }
        }
    }

    private ProductResponse buildProductResponse(Product product) {
        ProductResponse response = modelMapper.map(product, ProductResponse.class);

        // Resolve category name
        if (product.getCategoryId() != null) {
            String categoryName = categoryRepository.findByIdAndShopIdAndIsActiveTrue(product.getCategoryId(), product.getShopId())
                    .map(Category::getCategoryName)
                    .orElse(null);
            response.setCategoryName(categoryName);
        }

        // Fetch variants with stock
        List<ProductVariant> variants = productVariantRepository
                .findByProductIdAndShopIdAndIsActiveTrueOrderBySortOrderAsc(product.getId(), product.getShopId());
        List<ProductVariantResponse> variantResponses = variants.stream()
                .map(productVariantService::buildVariantResponse)
                .toList();
        response.setVariants(variantResponses);

        return response;
    }

    private String generateProductCode(UUID shopId) {
        long count = productRepository.countByShopId(shopId);
        String code;
        do {
            count++;
            code = String.format("PRD-%04d", count);
        } while (productRepository.existsByShopIdAndProductCode(shopId, code));
        return code;
    }

}
