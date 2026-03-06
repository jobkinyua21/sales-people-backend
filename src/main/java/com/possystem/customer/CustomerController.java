package com.possystem.customer;

import com.possystem.common.ApiResponse;
import com.possystem.common.FetchRequest;
import com.possystem.common.ListResponse;
import com.possystem.customer.payment.CustomerPaymentFetchRequest;
import com.possystem.customer.payment.CustomerPaymentRequest;
import com.possystem.customer.payment.CustomerPaymentResponse;
import com.possystem.customer.payment.CustomerPaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/customers")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService customerService;
    private final CustomerPaymentService customerPaymentService;

    @PostMapping("/save")
    @PreAuthorize("hasAnyRole('SYSTEM_OWNER', 'TENANT_ADMIN') or hasAuthority('CUSTOMERS_CREATE') or hasAuthority('CUSTOMERS_EDIT')")
    public ResponseEntity<ApiResponse<CustomerResponse>> save(
            @Valid @RequestBody CustomerRequest request) {
        CustomerResponse response = customerService.save(request);
        String message = request.getId() != null ? "Customer updated" : "Customer created";
        HttpStatus status = request.getId() != null ? HttpStatus.OK : HttpStatus.CREATED;

        if (status == HttpStatus.CREATED) {
            return ResponseEntity.status(status).body(ApiResponse.created(response, message));
        }
        return ResponseEntity.ok(ApiResponse.success(response, message));
    }

    @PostMapping("/fetch")
    @PreAuthorize("hasAnyRole('SYSTEM_OWNER', 'TENANT_ADMIN') or hasAuthority('CUSTOMERS_VIEW')")
    public ResponseEntity<ListResponse<CustomerResponse>> fetch(@RequestBody FetchRequest request) {
        ListResponse<CustomerResponse> response = customerService.fetch(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/payment")
    @PreAuthorize("hasAnyRole('SYSTEM_OWNER', 'TENANT_ADMIN') or hasAuthority('CUSTOMERS_EDIT') or hasAuthority('PAYMENTS_CREATE')")
    public ResponseEntity<ApiResponse<CustomerPaymentResponse>> addPayment(
            @Valid @RequestBody CustomerPaymentRequest request) {
        CustomerPaymentResponse response = customerPaymentService.addPayment(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(response, "Payment recorded"));
    }

    @PostMapping("/payments/fetch")
    @PreAuthorize("hasAnyRole('SYSTEM_OWNER', 'TENANT_ADMIN') or hasAuthority('CUSTOMERS_VIEW')")
    public ResponseEntity<ApiResponse<List<CustomerPaymentResponse>>> fetchPayments(
            @RequestBody CustomerPaymentFetchRequest request) {
        List<CustomerPaymentResponse> payments = customerPaymentService.fetchPayments(request.getCustomerId());
        return ResponseEntity.ok(ApiResponse.success(payments));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SYSTEM_OWNER', 'TENANT_ADMIN') or hasAuthority('CUSTOMERS_DELETE')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        customerService.delete(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Customer deleted"));
    }

    @DeleteMapping("/bulk")
    @PreAuthorize("hasAnyRole('SYSTEM_OWNER', 'TENANT_ADMIN') or hasAuthority('CUSTOMERS_DELETE')")
    public ResponseEntity<ApiResponse<Void>> bulkDelete(@RequestBody List<UUID> ids) {
        int count = customerService.bulkDelete(ids);
        return ResponseEntity.ok(ApiResponse.success(null, count + " customers deleted"));
    }
}
