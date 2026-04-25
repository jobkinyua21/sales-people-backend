package com.salespeople.customer;

import com.salespeople.common.ApiResponse;
import com.salespeople.common.ListResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/customers")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService customerService;

    @PostMapping("/save")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<CustomerResponse>> save(@Valid @RequestBody CustomerRequest request) {
        CustomerResponse response = customerService.save(request);
        String message = request.getCustomerId() != null ? "Customer updated" : "Customer created";
        HttpStatus status = request.getCustomerId() != null ? HttpStatus.OK : HttpStatus.CREATED;

        if (status == HttpStatus.CREATED) {
            return ResponseEntity.status(status).body(ApiResponse.created(response, message));
        }
        return ResponseEntity.ok(ApiResponse.success(response, message));
    }

    @PostMapping("/fetch")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ListResponse<CustomerResponse>> fetch(@RequestBody CustomerFetchRequest request) {
        return ResponseEntity.ok(customerService.fetch(request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('CUSTOMERS_DELETE')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        customerService.delete(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Customer deleted"));
    }
}
