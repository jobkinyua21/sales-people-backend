package com.possystem.inventory;

import com.possystem.common.FetchRequest;
import com.possystem.common.ListResponse;
import com.possystem.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;

    // ==================== CRUD ====================

    @Transactional
    public CategoryResponse save(CategoryRequest request) {
        UUID shopId = getCurrentShopId();

        if (request.getId() != null) {
            return updateCategory(request, shopId);
        }
        return createCategory(request, shopId);
    }

    public ListResponse<CategoryResponse> fetch(FetchRequest request) {
        UUID shopId = getCurrentShopId();

        if (request.getId() != null) {
            Category category = categoryRepository.findByIdAndShopIdAndIsActiveTrue(request.getId(), shopId)
                    .orElseThrow(() -> new IllegalArgumentException("Category not found"));
            List<CategoryResponse> result = List.of(buildCategoryResponse(category));
            return ListResponse.of(result);
        }

        String search = request.getSearch();
        Integer limit = request.getLimit();

        if (limit == null) {
            List<Category> all = categoryRepository.searchAll(shopId, search);
            List<CategoryResponse> responses = all.stream()
                    .map(this::buildCategoryResponse)
                    .toList();
            return ListResponse.of(responses);
        }

        PageRequest pageRequest = PageRequest.of(request.getStart(), limit);
        Page<Category> page = categoryRepository.searchAll(shopId, search, pageRequest);
        Page<CategoryResponse> responsePage = page.map(this::buildCategoryResponse);
        return ListResponse.from(responsePage);
    }

    @Transactional
    public void delete(UUID id) {
        UUID shopId = getCurrentShopId();

        Category category = categoryRepository.findByIdAndShopIdAndIsActiveTrue(id, shopId)
                .orElseThrow(() -> new IllegalArgumentException("Category not found"));

        category.setIsActive(false);
        category.setStatus(CategoryStatus.INACTIVE);
        categoryRepository.save(category);
    }

    // ==================== CREATE / UPDATE ====================

    private CategoryResponse createCategory(CategoryRequest request, UUID shopId) {
        if (categoryRepository.existsByShopIdAndCategoryNameIgnoreCaseAndIsActiveTrue(shopId, request.getCategoryName())) {
            throw new IllegalArgumentException("A category with this name already exists");
        }

        Category category = Category.builder()
                .shopId(shopId)
                .categoryCode(generateCategoryCode(shopId))
                .categoryName(request.getCategoryName())
                .description(request.getDescription())
                .imageUrl(request.getImageUrl())
                .sortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0)
                .status(request.getStatus() != null ? request.getStatus() : CategoryStatus.ACTIVE)
                .isActive(true)
                .build();

        Category saved = categoryRepository.save(category);
        return buildCategoryResponse(saved);
    }

    private CategoryResponse updateCategory(CategoryRequest request, UUID shopId) {
        Category category = categoryRepository.findByIdAndShopIdAndIsActiveTrue(request.getId(), shopId)
                .orElseThrow(() -> new IllegalArgumentException("Category not found"));

        if (!category.getCategoryName().equalsIgnoreCase(request.getCategoryName())) {
            if (categoryRepository.existsByShopIdAndCategoryNameIgnoreCaseAndIsActiveTrueAndIdNot(shopId, request.getCategoryName(), category.getId())) {
                throw new IllegalArgumentException("A category with this name already exists");
            }
        }

        category.setCategoryName(request.getCategoryName());
        category.setDescription(request.getDescription());
        category.setImageUrl(request.getImageUrl());
        if (request.getSortOrder() != null) {
            category.setSortOrder(request.getSortOrder());
        }
        if (request.getStatus() != null) {
            category.setStatus(request.getStatus());
        }

        Category saved = categoryRepository.save(category);
        return buildCategoryResponse(saved);
    }

    // ==================== HELPERS ====================

    private CategoryResponse buildCategoryResponse(Category category) {
        return CategoryResponse.builder()
                .id(category.getId())
                .shopId(category.getShopId())
                .categoryCode(category.getCategoryCode())
                .categoryName(category.getCategoryName())
                .description(category.getDescription())
                .imageUrl(category.getImageUrl())
                .sortOrder(category.getSortOrder())
                .status(category.getStatus())
                .isActive(category.getIsActive())
                .createdAt(category.getCreatedAt())
                .updatedAt(category.getUpdatedAt())
                .build();
    }

    private String generateCategoryCode(UUID shopId) {
        long count = categoryRepository.countByShopId(shopId);
        String code;
        do {
            count++;
            code = String.format("CAT-%04d", count);
        } while (categoryRepository.existsByShopIdAndCategoryCode(shopId, code));
        return code;
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
