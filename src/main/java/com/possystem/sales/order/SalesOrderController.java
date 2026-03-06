package com.possystem.sales;

import com.possystem.common.ApiResponse;
import com.possystem.common.ListResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/sales/orders")
@RequiredArgsConstructor
public class SalesOrderController {

    private final SalesOrderService salesOrderService;
    private final SalesReceiptService salesReceiptService;

    @PostMapping("/save")
    @PreAuthorize("hasAnyRole('SYSTEM_OWNER', 'TENANT_ADMIN') or hasAuthority('SALES_ORDERS_CREATE') or hasAuthority('SALES_ORDERS_EDIT')")
    public ResponseEntity<ApiResponse<SalesOrderResponse>> save(
            @Valid @RequestBody SalesOrderRequest request) {
        SalesOrderResponse response = salesOrderService.save(request);
        String message = request.getId() != null ? "Order updated" : "Order created";
        HttpStatus status = request.getId() != null ? HttpStatus.OK : HttpStatus.CREATED;

        if (status == HttpStatus.CREATED) {
            return ResponseEntity.status(status).body(ApiResponse.created(response, message));
        }
        return ResponseEntity.ok(ApiResponse.success(response, message));
    }

    @PostMapping("/fetch")
    @PreAuthorize("hasAnyRole('SYSTEM_OWNER', 'TENANT_ADMIN') or hasAuthority('SALES_ORDERS_VIEW')")
    public ResponseEntity<ListResponse<SalesOrderResponse>> fetch(
            @RequestBody SalesOrderFetchRequest request) {
        ListResponse<SalesOrderResponse> response = salesOrderService.fetch(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/complete")
    @PreAuthorize("hasAnyRole('SYSTEM_OWNER', 'TENANT_ADMIN') or hasAuthority('SALES_ORDERS_MANAGE')")
    public ResponseEntity<ApiResponse<SalesOrderResponse>> complete(
            @Valid @RequestBody SalesOrderActionRequest request) {
        SalesOrderResponse response = salesOrderService.completeOrder(request.getId());
        return ResponseEntity.ok(ApiResponse.success(response, "Order completed"));
    }

    @PostMapping("/cancel")
    @PreAuthorize("hasAnyRole('SYSTEM_OWNER', 'TENANT_ADMIN') or hasAuthority('SALES_ORDERS_MANAGE')")
    public ResponseEntity<ApiResponse<SalesOrderResponse>> cancel(
            @Valid @RequestBody SalesOrderActionRequest request) {
        SalesOrderResponse response = salesOrderService.cancelOrder(request.getId());
        return ResponseEntity.ok(ApiResponse.success(response, "Order cancelled"));
    }

    @PostMapping("/payment")
    @PreAuthorize("hasAnyRole('SYSTEM_OWNER', 'TENANT_ADMIN') or hasAuthority('SALES_ORDERS_CREATE') or hasAuthority('SALES_ORDERS_MANAGE')")
    public ResponseEntity<ApiResponse<SalesOrderResponse>> addPayment(
            @Valid @RequestBody AddPaymentRequest request) {
        SalesOrderResponse response = salesOrderService.addPayment(request);
        return ResponseEntity.ok(ApiResponse.success(response, "Payment added"));
    }

    @PostMapping("/receipt")
    @PreAuthorize("hasAnyRole('SYSTEM_OWNER', 'TENANT_ADMIN') or hasAuthority('SALES_ORDERS_VIEW')")
    public ResponseEntity<byte[]> downloadReceipt(@RequestBody SalesOrderActionRequest request) {
        byte[] pdf = salesReceiptService.generateReceipt(request.getId());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", "receipt.pdf");

        return new ResponseEntity<>(pdf, headers, HttpStatus.OK);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SYSTEM_OWNER', 'TENANT_ADMIN') or hasAuthority('SALES_ORDERS_DELETE')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        salesOrderService.delete(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Order deleted"));
    }

    @DeleteMapping("/bulk")
    @PreAuthorize("hasAnyRole('SYSTEM_OWNER', 'TENANT_ADMIN') or hasAuthority('SALES_ORDERS_DELETE')")
    public ResponseEntity<ApiResponse<Void>> bulkDelete(@RequestBody List<UUID> ids) {
        int count = salesOrderService.bulkDelete(ids);
        return ResponseEntity.ok(ApiResponse.success(null, count + " orders deleted"));
    }
}
