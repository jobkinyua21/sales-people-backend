package com.possystem.expense;

import com.possystem.common.ListResponse;
import com.possystem.expense.category.ExpenseCategory;
import com.possystem.expense.category.ExpenseCategoryRepository;
import com.possystem.expense.enums.ExpenseStatus;
import com.possystem.security.SecurityContextUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ExpenseService {

    private final ExpenseRepository expenseRepository;
    private final ExpenseCategoryRepository expenseCategoryRepository;

    // ==================== CRUD ====================

    @Transactional
    public ExpenseResponse save(ExpenseRequest request) {
        UUID shopId = SecurityContextUtil.getCurrentShopId();

        if (request.getId() != null) {
            return updateExpense(request, shopId);
        }
        return createExpense(request, shopId);
    }

    public ListResponse<ExpenseResponse> fetch(ExpenseFetchRequest request) {
        UUID shopId = SecurityContextUtil.getCurrentShopId();

        if (request.getId() != null) {
            Expense expense = expenseRepository.findByIdAndShopIdAndIsActiveTrue(request.getId(), shopId)
                    .orElseThrow(() -> new IllegalArgumentException("Expense not found"));
            List<ExpenseResponse> result = List.of(buildResponse(expense, shopId));
            return ListResponse.of(result);
        }

        String search = request.getSearch();
        String expenseStatus = request.getExpenseStatus() != null ? request.getExpenseStatus().name() : null;
        UUID expenseCategoryId = request.getExpenseCategoryId();
        String paymentMethod = request.getPaymentMethod() != null ? request.getPaymentMethod().name() : null;
        LocalDateTime dateFrom = request.getDateFrom();
        LocalDateTime dateTo = request.getDateTo();
        Integer limit = request.getLimit();

        if (limit == null) {
            List<Expense> all = expenseRepository.searchFilteredUnpaged(
                    shopId, search, expenseStatus, expenseCategoryId, paymentMethod, dateFrom, dateTo);
            List<ExpenseResponse> responses = all.stream()
                    .map(e -> buildResponse(e, shopId))
                    .toList();
            return ListResponse.of(responses);
        }

        PageRequest pageRequest = PageRequest.of(request.getStart(), limit);
        Page<Expense> page = expenseRepository.searchFiltered(
                shopId, search, expenseStatus, expenseCategoryId, paymentMethod, dateFrom, dateTo, pageRequest);
        Page<ExpenseResponse> responsePage = page.map(e -> buildResponse(e, shopId));
        return ListResponse.from(responsePage);
    }

    // ==================== SUMMARY / ANALYTICS ====================

    public ExpenseSummaryResponse getSummary(ExpenseSummaryRequest request) {
        UUID shopId = SecurityContextUtil.getCurrentShopId();

        LocalDate dateFrom = request.getDateFrom();
        LocalDate dateTo = request.getDateTo();

        // Default: last 12 months
        if (dateTo == null) {
            dateTo = LocalDate.now();
        }
        if (dateFrom == null) {
            dateFrom = dateTo.minusMonths(11).withDayOfMonth(1);
        }

        // Monthly totals (for the main chart line)
        List<ExpenseMonthlySummary> monthlyTotals = expenseRepository.getMonthlyTotals(shopId, dateFrom, dateTo);

        // Per-category monthly breakdown (for filter tabs)
        List<ExpenseCategorySummary> categoryMonthly = expenseRepository.getMonthlyCategoryTotals(shopId, dateFrom, dateTo);

        // Build monthly totals
        List<ExpenseSummaryResponse.MonthlyTotal> monthlyList = monthlyTotals.stream()
                .map(m -> ExpenseSummaryResponse.MonthlyTotal.builder()
                        .year(m.getYear())
                        .month(m.getMonth())
                        .total(m.getTotal())
                        .build())
                .toList();

        // Calculate grand total
        BigDecimal totalExpenses = monthlyList.stream()
                .map(ExpenseSummaryResponse.MonthlyTotal::getTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Group category data by categoryId
        Map<UUID, List<ExpenseCategorySummary>> byCategoryId = categoryMonthly.stream()
                .collect(Collectors.groupingBy(ExpenseCategorySummary::getCategoryId));

        List<ExpenseSummaryResponse.CategoryBreakdown> categoryBreakdowns = byCategoryId.entrySet().stream()
                .map(entry -> {
                    List<ExpenseCategorySummary> items = entry.getValue();
                    String categoryName = items.get(0).getCategoryName();

                    List<ExpenseSummaryResponse.MonthlyTotal> catMonthly = items.stream()
                            .map(i -> ExpenseSummaryResponse.MonthlyTotal.builder()
                                    .year(i.getYear())
                                    .month(i.getMonth())
                                    .total(i.getTotal())
                                    .build())
                            .toList();

                    BigDecimal catTotal = catMonthly.stream()
                            .map(ExpenseSummaryResponse.MonthlyTotal::getTotal)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    return ExpenseSummaryResponse.CategoryBreakdown.builder()
                            .categoryId(entry.getKey())
                            .categoryName(categoryName)
                            .total(catTotal)
                            .monthlyTotals(catMonthly)
                            .build();
                })
                .sorted((a, b) -> b.getTotal().compareTo(a.getTotal()))
                .toList();

        return ExpenseSummaryResponse.builder()
                .totalExpenses(totalExpenses)
                .monthlyTotals(monthlyList)
                .categoryBreakdowns(categoryBreakdowns)
                .build();
    }

    // ==================== STATUS TRANSITIONS ====================

    @Transactional
    public ExpenseResponse approveExpense(UUID expenseId) {
        UUID shopId = SecurityContextUtil.getCurrentShopId();
        Expense expense = expenseRepository.findByIdAndShopIdAndIsActiveTrue(expenseId, shopId)
                .orElseThrow(() -> new IllegalArgumentException("Expense not found"));

        if (expense.getExpenseStatus() != ExpenseStatus.PENDING) {
            throw new IllegalArgumentException("Only PENDING expenses can be approved");
        }

        expense.setExpenseStatus(ExpenseStatus.APPROVED);
        expense.setApprovedBy(SecurityContextUtil.getCurrentUserId());
        expense.setApprovedAt(LocalDateTime.now());

        Expense saved = expenseRepository.save(expense);
        return buildResponse(saved, shopId);
    }

    @Transactional
    public ExpenseResponse rejectExpense(UUID expenseId, String rejectionReason) {
        UUID shopId = SecurityContextUtil.getCurrentShopId();
        Expense expense = expenseRepository.findByIdAndShopIdAndIsActiveTrue(expenseId, shopId)
                .orElseThrow(() -> new IllegalArgumentException("Expense not found"));

        if (expense.getExpenseStatus() != ExpenseStatus.PENDING) {
            throw new IllegalArgumentException("Only PENDING expenses can be rejected");
        }

        expense.setExpenseStatus(ExpenseStatus.REJECTED);
        expense.setRejectedBy(SecurityContextUtil.getCurrentUserId());
        expense.setRejectedAt(LocalDateTime.now());
        expense.setRejectionReason(rejectionReason);

        Expense saved = expenseRepository.save(expense);
        return buildResponse(saved, shopId);
    }

    @Transactional
    public ExpenseResponse markAsPaid(UUID expenseId) {
        UUID shopId = SecurityContextUtil.getCurrentShopId();
        Expense expense = expenseRepository.findByIdAndShopIdAndIsActiveTrue(expenseId, shopId)
                .orElseThrow(() -> new IllegalArgumentException("Expense not found"));

        if (expense.getExpenseStatus() != ExpenseStatus.APPROVED) {
            throw new IllegalArgumentException("Only APPROVED expenses can be marked as paid");
        }

        expense.setExpenseStatus(ExpenseStatus.PAID);
        expense.setPaidAt(LocalDateTime.now());

        Expense saved = expenseRepository.save(expense);
        return buildResponse(saved, shopId);
    }

    @Transactional
    public ExpenseResponse cancelExpense(UUID expenseId) {
        UUID shopId = SecurityContextUtil.getCurrentShopId();
        Expense expense = expenseRepository.findByIdAndShopIdAndIsActiveTrue(expenseId, shopId)
                .orElseThrow(() -> new IllegalArgumentException("Expense not found"));

        if (expense.getExpenseStatus() == ExpenseStatus.PAID) {
            throw new IllegalArgumentException("PAID expenses cannot be cancelled");
        }
        if (expense.getExpenseStatus() == ExpenseStatus.CANCELLED) {
            throw new IllegalArgumentException("Expense is already cancelled");
        }

        expense.setExpenseStatus(ExpenseStatus.CANCELLED);
        expense.setCancelledAt(LocalDateTime.now());

        Expense saved = expenseRepository.save(expense);
        return buildResponse(saved, shopId);
    }

    // ==================== DELETE ====================

    @Transactional
    public void delete(UUID id) {
        UUID shopId = SecurityContextUtil.getCurrentShopId();
        Expense expense = expenseRepository.findByIdAndShopIdAndIsActiveTrue(id, shopId)
                .orElseThrow(() -> new IllegalArgumentException("Expense not found"));

        if (expense.getExpenseStatus() != ExpenseStatus.PENDING
                && expense.getExpenseStatus() != ExpenseStatus.REJECTED
                && expense.getExpenseStatus() != ExpenseStatus.CANCELLED) {
            throw new IllegalArgumentException("Only PENDING, REJECTED, or CANCELLED expenses can be deleted");
        }

        expense.setIsActive(false);
        expenseRepository.save(expense);
    }

    @Transactional
    public int bulkDelete(List<UUID> ids) {
        UUID shopId = SecurityContextUtil.getCurrentShopId();
        List<Expense> expenses = expenseRepository.findAllByIdInAndShopIdAndIsActiveTrue(ids, shopId);
        if (expenses.isEmpty()) {
            throw new IllegalArgumentException("No expenses found for the given IDs");
        }

        for (Expense expense : expenses) {
            if (expense.getExpenseStatus() != ExpenseStatus.PENDING
                    && expense.getExpenseStatus() != ExpenseStatus.REJECTED
                    && expense.getExpenseStatus() != ExpenseStatus.CANCELLED) {
                throw new IllegalArgumentException(
                        "Expense " + expense.getExpenseNumber() + " cannot be deleted — only PENDING, REJECTED, or CANCELLED expenses");
            }
            expense.setIsActive(false);
        }
        expenseRepository.saveAll(expenses);
        return expenses.size();
    }

    // ==================== CREATE / UPDATE ====================

    private ExpenseResponse createExpense(ExpenseRequest request, UUID shopId) {
        // Validate category exists
        expenseCategoryRepository.findByIdAndShopIdAndIsActiveTrue(request.getExpenseCategoryId(), shopId)
                .orElseThrow(() -> new IllegalArgumentException("Expense category not found"));

        String expenseNumber = generateExpenseNumber(shopId);

        Expense expense = Expense.builder()
                .shopId(shopId)
                .expenseNumber(expenseNumber)
                .expenseCategoryId(request.getExpenseCategoryId())
                .amount(request.getAmount())
                .expenseDate(request.getExpenseDate() != null ? request.getExpenseDate() : LocalDate.now())
                .paymentMethod(request.getPaymentMethod())
                .vendor(request.getVendor())
                .referenceNumber(request.getReferenceNumber())
                .description(request.getDescription())
                .notes(request.getNotes())
                .recordedBy(SecurityContextUtil.getCurrentUserId())
                .build();

        Expense saved = expenseRepository.save(expense);
        return buildResponse(saved, shopId);
    }

    private ExpenseResponse updateExpense(ExpenseRequest request, UUID shopId) {
        Expense expense = expenseRepository.findByIdAndShopIdAndIsActiveTrue(request.getId(), shopId)
                .orElseThrow(() -> new IllegalArgumentException("Expense not found"));

        if (expense.getExpenseStatus() != ExpenseStatus.PENDING
                && expense.getExpenseStatus() != ExpenseStatus.REJECTED) {
            throw new IllegalArgumentException("Only PENDING or REJECTED expenses can be updated");
        }

        // Validate category if changed
        if (!expense.getExpenseCategoryId().equals(request.getExpenseCategoryId())) {
            expenseCategoryRepository.findByIdAndShopIdAndIsActiveTrue(request.getExpenseCategoryId(), shopId)
                    .orElseThrow(() -> new IllegalArgumentException("Expense category not found"));
        }

        expense.setExpenseCategoryId(request.getExpenseCategoryId());
        expense.setAmount(request.getAmount());
        expense.setExpenseDate(request.getExpenseDate() != null ? request.getExpenseDate() : expense.getExpenseDate());
        expense.setPaymentMethod(request.getPaymentMethod());
        expense.setVendor(request.getVendor());
        expense.setReferenceNumber(request.getReferenceNumber());
        expense.setDescription(request.getDescription());
        expense.setNotes(request.getNotes());

        // If rejected and re-submitted, reset back to PENDING
        if (expense.getExpenseStatus() == ExpenseStatus.REJECTED) {
            expense.setExpenseStatus(ExpenseStatus.PENDING);
            expense.setRejectedBy(null);
            expense.setRejectedAt(null);
            expense.setRejectionReason(null);
        }

        Expense saved = expenseRepository.save(expense);
        return buildResponse(saved, shopId);
    }

    // ==================== RESPONSE BUILDER ====================

    private ExpenseResponse buildResponse(Expense expense, UUID shopId) {
        String categoryName = null;
        ExpenseCategory category = expenseCategoryRepository
                .findByIdAndShopIdAndIsActiveTrue(expense.getExpenseCategoryId(), shopId)
                .orElse(null);
        if (category != null) {
            categoryName = category.getCategoryName();
        }

        return ExpenseResponse.builder()
                .id(expense.getId())
                .expenseNumber(expense.getExpenseNumber())
                .expenseCategoryId(expense.getExpenseCategoryId())
                .categoryName(categoryName)
                .amount(expense.getAmount())
                .expenseDate(expense.getExpenseDate())
                .paymentMethod(expense.getPaymentMethod())
                .expenseStatus(expense.getExpenseStatus())
                .vendor(expense.getVendor())
                .referenceNumber(expense.getReferenceNumber())
                .description(expense.getDescription())
                .notes(expense.getNotes())
                .recordedBy(expense.getRecordedBy())
                .approvedBy(expense.getApprovedBy())
                .approvedAt(expense.getApprovedAt())
                .rejectedBy(expense.getRejectedBy())
                .rejectedAt(expense.getRejectedAt())
                .rejectionReason(expense.getRejectionReason())
                .paidAt(expense.getPaidAt())
                .cancelledAt(expense.getCancelledAt())
                .createdAt(expense.getCreatedAt())
                .updatedAt(expense.getUpdatedAt())
                .build();
    }

    // ==================== HELPERS ====================

    private String generateExpenseNumber(UUID shopId) {
        long count = expenseRepository.countByShopId(shopId);
        String code;
        do {
            count++;
            code = String.format("EXP-%05d", count);
        } while (expenseRepository.existsByShopIdAndExpenseNumber(shopId, code));
        return code;
    }

}
