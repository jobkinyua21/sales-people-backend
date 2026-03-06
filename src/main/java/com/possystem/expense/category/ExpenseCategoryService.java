package com.possystem.expense.category;

import com.possystem.common.FetchRequest;
import com.possystem.common.ListResponse;
import com.possystem.expense.enums.ExpenseCategoryStatus;
import com.possystem.security.SecurityContextUtil;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ExpenseCategoryService {

    private final ExpenseCategoryRepository expenseCategoryRepository;
    private final ModelMapper modelMapper;

    // ==================== CRUD ====================

    @Transactional
    public ExpenseCategoryResponse save(ExpenseCategoryRequest request) {
        UUID shopId = SecurityContextUtil.getCurrentShopId();

        if (request.getId() != null) {
            return updateCategory(request, shopId);
        }
        return createCategory(request, shopId);
    }

    public ListResponse<ExpenseCategoryResponse> fetch(FetchRequest request) {
        UUID shopId = SecurityContextUtil.getCurrentShopId();

        if (request.getId() != null) {
            ExpenseCategory category = expenseCategoryRepository.findByIdAndShopIdAndIsActiveTrue(request.getId(), shopId)
                    .orElseThrow(() -> new IllegalArgumentException("Expense category not found"));
            List<ExpenseCategoryResponse> result = List.of(modelMapper.map(category, ExpenseCategoryResponse.class));
            return ListResponse.of(result);
        }

        String search = request.getSearch();
        Integer limit = request.getLimit();

        if (limit == null) {
            List<ExpenseCategory> all = expenseCategoryRepository.searchAll(shopId, search);
            List<ExpenseCategoryResponse> responses = all.stream()
                    .map(c -> modelMapper.map(c, ExpenseCategoryResponse.class))
                    .toList();
            return ListResponse.of(responses);
        }

        PageRequest pageRequest = PageRequest.of(request.getStart(), limit);
        Page<ExpenseCategory> page = expenseCategoryRepository.searchAll(shopId, search, pageRequest);
        Page<ExpenseCategoryResponse> responsePage = page.map(c -> modelMapper.map(c, ExpenseCategoryResponse.class));
        return ListResponse.from(responsePage);
    }

    @Transactional
    public void delete(UUID id) {
        UUID shopId = SecurityContextUtil.getCurrentShopId();
        ExpenseCategory category = expenseCategoryRepository.findByIdAndShopIdAndIsActiveTrue(id, shopId)
                .orElseThrow(() -> new IllegalArgumentException("Expense category not found"));
        category.setIsActive(false);
        category.setStatus(ExpenseCategoryStatus.INACTIVE);
        expenseCategoryRepository.save(category);
    }

    @Transactional
    public int bulkDelete(List<UUID> ids) {
        UUID shopId = SecurityContextUtil.getCurrentShopId();
        List<ExpenseCategory> categories = expenseCategoryRepository.findAllByIdInAndShopIdAndIsActiveTrue(ids, shopId);
        if (categories.isEmpty()) {
            throw new IllegalArgumentException("No expense categories found for the given IDs");
        }
        for (ExpenseCategory category : categories) {
            category.setIsActive(false);
            category.setStatus(ExpenseCategoryStatus.INACTIVE);
        }
        expenseCategoryRepository.saveAll(categories);
        return categories.size();
    }

    // ==================== CREATE / UPDATE ====================

    private ExpenseCategoryResponse createCategory(ExpenseCategoryRequest request, UUID shopId) {
        if (expenseCategoryRepository.existsByShopIdAndCategoryNameIgnoreCaseAndIsActiveTrue(shopId, request.getCategoryName())) {
            throw new IllegalArgumentException("An expense category with this name already exists");
        }

        ExpenseCategory category = modelMapper.map(request, ExpenseCategory.class);
        category.setShopId(shopId);
        category.setCategoryCode(generateCategoryCode(shopId));
        if (category.getStatus() == null) category.setStatus(ExpenseCategoryStatus.ACTIVE);

        ExpenseCategory saved = expenseCategoryRepository.save(category);
        return modelMapper.map(saved, ExpenseCategoryResponse.class);
    }

    private ExpenseCategoryResponse updateCategory(ExpenseCategoryRequest request, UUID shopId) {
        ExpenseCategory category = expenseCategoryRepository.findByIdAndShopIdAndIsActiveTrue(request.getId(), shopId)
                .orElseThrow(() -> new IllegalArgumentException("Expense category not found"));

        if (request.getCategoryName() != null && !category.getCategoryName().equalsIgnoreCase(request.getCategoryName())) {
            if (expenseCategoryRepository.existsByShopIdAndCategoryNameIgnoreCaseAndIsActiveTrueAndIdNot(
                    shopId, request.getCategoryName(), category.getId())) {
                throw new IllegalArgumentException("An expense category with this name already exists");
            }
        }

        modelMapper.map(request, category);

        ExpenseCategory saved = expenseCategoryRepository.save(category);
        return modelMapper.map(saved, ExpenseCategoryResponse.class);
    }

    // ==================== HELPERS ====================

    private String generateCategoryCode(UUID shopId) {
        long count = expenseCategoryRepository.countByShopId(shopId);
        String code;
        do {
            count++;
            code = String.format("ECAT-%04d", count);
        } while (expenseCategoryRepository.existsByShopIdAndCategoryCode(shopId, code));
        return code;
    }

}
