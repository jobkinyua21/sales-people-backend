package com.possystem.inventory;

import com.possystem.common.FetchRequest;
import com.possystem.common.ListResponse;
import com.possystem.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
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
    private final ModelMapper modelMapper;

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
            List<CategoryResponse> result = List.of(modelMapper.map(category, CategoryResponse.class));
            return ListResponse.of(result);
        }

        String search = request.getSearch();
        Integer limit = request.getLimit();

        if (limit == null) {
            List<Category> all = categoryRepository.searchAll(shopId, search);
            List<CategoryResponse> responses = all.stream()
                    .map(c -> modelMapper.map(c, CategoryResponse.class))
                    .toList();
            return ListResponse.of(responses);
        }

        PageRequest pageRequest = PageRequest.of(request.getStart(), limit);
        Page<Category> page = categoryRepository.searchAll(shopId, search, pageRequest);
        Page<CategoryResponse> responsePage = page.map(c -> modelMapper.map(c, CategoryResponse.class));
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

        Category category = modelMapper.map(request, Category.class);
        category.setShopId(shopId);
        category.setCategoryCode(generateCategoryCode(shopId));
        if (category.getSortOrder() == null) category.setSortOrder(0);
        if (category.getStatus() == null) category.setStatus(CategoryStatus.ACTIVE);
        category.setIsActive(true);

        Category saved = categoryRepository.save(category);
        return modelMapper.map(saved, CategoryResponse.class);
    }

    private CategoryResponse updateCategory(CategoryRequest request, UUID shopId) {
        Category category = categoryRepository.findByIdAndShopIdAndIsActiveTrue(request.getId(), shopId)
                .orElseThrow(() -> new IllegalArgumentException("Category not found"));

        if (request.getCategoryName() != null && !category.getCategoryName().equalsIgnoreCase(request.getCategoryName())) {
            if (categoryRepository.existsByShopIdAndCategoryNameIgnoreCaseAndIsActiveTrueAndIdNot(shopId, request.getCategoryName(), category.getId())) {
                throw new IllegalArgumentException("A category with this name already exists");
            }
        }

        modelMapper.map(request, category);

        Category saved = categoryRepository.save(category);
        return modelMapper.map(saved, CategoryResponse.class);
    }

    // ==================== HELPERS ====================

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
