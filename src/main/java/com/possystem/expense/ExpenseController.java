package com.possystem.expense;

import com.possystem.common.ApiResponse;
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
@RequestMapping("/api/v1/expenses")
@RequiredArgsConstructor
@RequiresModule("EXPENSES")
public class ExpenseController {

    private final ExpenseService expenseService;

    @PostMapping("/save")
    @PreAuthorize("hasAnyRole('SYSTEM_OWNER', 'TENANT_ADMIN') or hasAuthority('EXPENSES_CREATE') or hasAuthority('EXPENSES_EDIT')")
    public ResponseEntity<ApiResponse<ExpenseResponse>> save(
            @Valid @RequestBody ExpenseRequest request) {
        ExpenseResponse response = expenseService.save(request);
        String message = request.getId() != null ? "Expense updated" : "Expense created";
        HttpStatus status = request.getId() != null ? HttpStatus.OK : HttpStatus.CREATED;

        if (status == HttpStatus.CREATED) {
            return ResponseEntity.status(status).body(ApiResponse.created(response, message));
        }
        return ResponseEntity.ok(ApiResponse.success(response, message));
    }

    @PostMapping("/fetch")
    @PreAuthorize("hasAnyRole('SYSTEM_OWNER', 'TENANT_ADMIN') or hasAuthority('EXPENSES_VIEW')")
    public ResponseEntity<ListResponse<ExpenseResponse>> fetch(
            @RequestBody ExpenseFetchRequest request) {
        ListResponse<ExpenseResponse> response = expenseService.fetch(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/summary")
    @PreAuthorize("hasAnyRole('SYSTEM_OWNER', 'TENANT_ADMIN') or hasAuthority('EXPENSES_VIEW')")
    public ResponseEntity<ApiResponse<ExpenseSummaryResponse>> summary(
            @RequestBody ExpenseSummaryRequest request) {
        ExpenseSummaryResponse response = expenseService.getSummary(request);
        return ResponseEntity.ok(ApiResponse.success(response, "Expense summary fetched"));
    }

    @PostMapping("/approve")
    @PreAuthorize("hasAnyRole('SYSTEM_OWNER', 'TENANT_ADMIN') or hasAuthority('EXPENSES_MANAGE')")
    public ResponseEntity<ApiResponse<ExpenseResponse>> approve(
            @Valid @RequestBody ExpenseActionRequest request) {
        ExpenseResponse response = expenseService.approveExpense(request.getId());
        return ResponseEntity.ok(ApiResponse.success(response, "Expense approved"));
    }

    @PostMapping("/reject")
    @PreAuthorize("hasAnyRole('SYSTEM_OWNER', 'TENANT_ADMIN') or hasAuthority('EXPENSES_MANAGE')")
    public ResponseEntity<ApiResponse<ExpenseResponse>> reject(
            @Valid @RequestBody ExpenseActionRequest request) {
        ExpenseResponse response = expenseService.rejectExpense(request.getId(), request.getRejectionReason());
        return ResponseEntity.ok(ApiResponse.success(response, "Expense rejected"));
    }

    @PostMapping("/mark-paid")
    @PreAuthorize("hasAnyRole('SYSTEM_OWNER', 'TENANT_ADMIN') or hasAuthority('EXPENSES_MANAGE')")
    public ResponseEntity<ApiResponse<ExpenseResponse>> markAsPaid(
            @Valid @RequestBody ExpenseActionRequest request) {
        ExpenseResponse response = expenseService.markAsPaid(request.getId());
        return ResponseEntity.ok(ApiResponse.success(response, "Expense marked as paid"));
    }

    @PostMapping("/cancel")
    @PreAuthorize("hasAnyRole('SYSTEM_OWNER', 'TENANT_ADMIN') or hasAuthority('EXPENSES_EDIT')")
    public ResponseEntity<ApiResponse<ExpenseResponse>> cancel(
            @Valid @RequestBody ExpenseActionRequest request) {
        ExpenseResponse response = expenseService.cancelExpense(request.getId());
        return ResponseEntity.ok(ApiResponse.success(response, "Expense cancelled"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SYSTEM_OWNER', 'TENANT_ADMIN') or hasAuthority('EXPENSES_DELETE')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        expenseService.delete(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Expense deleted"));
    }

    @DeleteMapping("/bulk")
    @PreAuthorize("hasAnyRole('SYSTEM_OWNER', 'TENANT_ADMIN') or hasAuthority('EXPENSES_DELETE')")
    public ResponseEntity<ApiResponse<Void>> bulkDelete(@RequestBody List<UUID> ids) {
        int count = expenseService.bulkDelete(ids);
        return ResponseEntity.ok(ApiResponse.success(null, count + " expense(s) deleted"));
    }
}
