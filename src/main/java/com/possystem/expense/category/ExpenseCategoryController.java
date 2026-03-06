package com.possystem.expense.category;

import com.possystem.common.ApiResponse;
import com.possystem.common.FetchRequest;
import com.possystem.common.ListResponse;
import com.possystem.security.RequiresModule;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/expense-categories")
@RequiredArgsConstructor
@RequiresModule("EXPENSES")
public class ExpenseCategoryController {

    private final ExpenseCategoryService expenseCategoryService;

    @PostMapping("/save")
    @PreAuthorize("hasAnyRole('SYSTEM_OWNER', 'TENANT_ADMIN') or hasAuthority('EXPENSE_CATEGORIES_CREATE') or hasAuthority('EXPENSE_CATEGORIES_EDIT')")
    public ResponseEntity<ApiResponse<ExpenseCategoryResponse>> save(
            @Valid @RequestBody ExpenseCategoryRequest request) {
        ExpenseCategoryResponse response = expenseCategoryService.save(request);
        String message = request.getId() != null ? "Expense category updated" : "Expense category created";
        HttpStatus status = request.getId() != null ? HttpStatus.OK : HttpStatus.CREATED;

        if (status == HttpStatus.CREATED) {
            return ResponseEntity.status(status).body(ApiResponse.created(response, message));
        }
        return ResponseEntity.ok(ApiResponse.success(response, message));
    }

    @PostMapping("/fetch")
    @PreAuthorize("hasAnyRole('SYSTEM_OWNER', 'TENANT_ADMIN') or hasAuthority('EXPENSE_CATEGORIES_VIEW')")
    public ResponseEntity<ListResponse<ExpenseCategoryResponse>> fetch(@RequestBody FetchRequest request) {
        ListResponse<ExpenseCategoryResponse> response = expenseCategoryService.fetch(request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SYSTEM_OWNER', 'TENANT_ADMIN') or hasAuthority('EXPENSE_CATEGORIES_DELETE')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        expenseCategoryService.delete(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Expense category deleted"));
    }

    @DeleteMapping("/bulk")
    @PreAuthorize("hasAnyRole('SYSTEM_OWNER', 'TENANT_ADMIN') or hasAuthority('EXPENSE_CATEGORIES_DELETE')")
    public ResponseEntity<ApiResponse<Void>> bulkDelete(@RequestBody List<UUID> ids) {
        int count = expenseCategoryService.bulkDelete(ids);
        return ResponseEntity.ok(ApiResponse.success(null, count + " expense category(s) deleted"));
    }
}
