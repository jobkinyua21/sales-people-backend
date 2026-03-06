package com.possystem.purchasing.invoice;

import com.possystem.common.ApiResponse;
import com.possystem.common.ListResponse;
import com.possystem.purchasing.payment.AddPurchasePaymentRequest;
import com.possystem.purchasing.payment.PurchasePaymentResponse;
import com.possystem.purchasing.payment.PurchasePaymentVoucherService;
import com.possystem.security.RequiresModule;
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
@RequestMapping("/api/v1/supplier-invoices")
@RequiredArgsConstructor
@RequiresModule("PURCHASING")
public class SupplierInvoiceController {

    private final SupplierInvoiceService supplierInvoiceService;
    private final PurchasePaymentVoucherService purchasePaymentVoucherService;

    @PostMapping("/save")
    @PreAuthorize("hasAnyRole('SYSTEM_OWNER', 'TENANT_ADMIN') or hasAuthority('SUPPLIER_INVOICES_CREATE') or hasAuthority('SUPPLIER_INVOICES_EDIT')")
    public ResponseEntity<ApiResponse<SupplierInvoiceResponse>> save(
            @Valid @RequestBody SupplierInvoiceRequest request) {
        SupplierInvoiceResponse response = supplierInvoiceService.save(request);
        String message = request.getId() != null ? "Invoice updated" : "Invoice created";
        HttpStatus status = request.getId() != null ? HttpStatus.OK : HttpStatus.CREATED;

        if (status == HttpStatus.CREATED) {
            return ResponseEntity.status(status).body(ApiResponse.created(response, message));
        }
        return ResponseEntity.ok(ApiResponse.success(response, message));
    }

    @PostMapping("/fetch")
    @PreAuthorize("hasAnyRole('SYSTEM_OWNER', 'TENANT_ADMIN') or hasAuthority('SUPPLIER_INVOICES_VIEW')")
    public ResponseEntity<ListResponse<SupplierInvoiceResponse>> fetch(
            @RequestBody SupplierInvoiceFetchRequest request) {
        ListResponse<SupplierInvoiceResponse> response = supplierInvoiceService.fetch(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/approve")
    @PreAuthorize("hasAnyRole('SYSTEM_OWNER', 'TENANT_ADMIN') or hasAuthority('SUPPLIER_INVOICES_APPROVE')")
    public ResponseEntity<ApiResponse<SupplierInvoiceResponse>> approve(
            @Valid @RequestBody SupplierInvoiceActionRequest request) {
        SupplierInvoiceResponse response = supplierInvoiceService.approve(request.getId());
        return ResponseEntity.ok(ApiResponse.success(response, "Invoice approved"));
    }

    @PostMapping("/cancel")
    @PreAuthorize("hasAnyRole('SYSTEM_OWNER', 'TENANT_ADMIN') or hasAuthority('SUPPLIER_INVOICES_EDIT')")
    public ResponseEntity<ApiResponse<SupplierInvoiceResponse>> cancel(
            @Valid @RequestBody SupplierInvoiceActionRequest request) {
        SupplierInvoiceResponse response = supplierInvoiceService.cancel(request.getId());
        return ResponseEntity.ok(ApiResponse.success(response, "Invoice cancelled"));
    }

    @PostMapping("/dispute")
    @PreAuthorize("hasAnyRole('SYSTEM_OWNER', 'TENANT_ADMIN') or hasAuthority('SUPPLIER_INVOICES_EDIT')")
    public ResponseEntity<ApiResponse<SupplierInvoiceResponse>> dispute(
            @Valid @RequestBody SupplierInvoiceActionRequest request) {
        SupplierInvoiceResponse response = supplierInvoiceService.dispute(request.getId());
        return ResponseEntity.ok(ApiResponse.success(response, "Invoice disputed"));
    }

    @PostMapping("/resolve")
    @PreAuthorize("hasAnyRole('SYSTEM_OWNER', 'TENANT_ADMIN') or hasAuthority('SUPPLIER_INVOICES_EDIT')")
    public ResponseEntity<ApiResponse<SupplierInvoiceResponse>> resolve(
            @Valid @RequestBody SupplierInvoiceActionRequest request) {
        SupplierInvoiceResponse response = supplierInvoiceService.resolve(request.getId());
        return ResponseEntity.ok(ApiResponse.success(response, "Invoice dispute resolved"));
    }

    @PostMapping("/payments/fetch")
    @PreAuthorize("hasAnyRole('SYSTEM_OWNER', 'TENANT_ADMIN') or hasAuthority('PURCHASE_PAYMENTS_VIEW')")
    public ResponseEntity<ApiResponse<List<PurchasePaymentResponse>>> fetchPayments(
            @RequestBody SupplierInvoiceActionRequest request) {
        List<PurchasePaymentResponse> payments = supplierInvoiceService.fetchPayments(request.getId());
        return ResponseEntity.ok(ApiResponse.success(payments));
    }

    @PostMapping("/payment")
    @PreAuthorize("hasAnyRole('SYSTEM_OWNER', 'TENANT_ADMIN') or hasAuthority('PURCHASE_PAYMENTS_CREATE')")
    public ResponseEntity<ApiResponse<SupplierInvoiceResponse>> addPayment(
            @Valid @RequestBody AddPurchasePaymentRequest request) {
        SupplierInvoiceResponse response = supplierInvoiceService.addPayment(request);
        return ResponseEntity.ok(ApiResponse.success(response, "Payment added"));
    }

    @PostMapping("/payment/voucher")
    @PreAuthorize("hasAnyRole('SYSTEM_OWNER', 'TENANT_ADMIN') or hasAuthority('PURCHASE_PAYMENTS_VIEW') or hasAuthority('PURCHASE_PAYMENTS_CREATE')")
    public ResponseEntity<byte[]> downloadVoucher(@RequestBody SupplierInvoiceActionRequest request) {
        byte[] pdf = purchasePaymentVoucherService.generateVoucher(request.getId());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", "payment-voucher.pdf");

        return new ResponseEntity<>(pdf, headers, HttpStatus.OK);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SYSTEM_OWNER', 'TENANT_ADMIN') or hasAuthority('SUPPLIER_INVOICES_DELETE')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        supplierInvoiceService.delete(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Invoice deleted"));
    }

    @DeleteMapping("/bulk")
    @PreAuthorize("hasAnyRole('SYSTEM_OWNER', 'TENANT_ADMIN') or hasAuthority('SUPPLIER_INVOICES_DELETE')")
    public ResponseEntity<ApiResponse<Void>> bulkDelete(@RequestBody List<UUID> ids) {
        supplierInvoiceService.bulkDelete(ids);
        return ResponseEntity.ok(ApiResponse.success(null, "Invoices deleted"));
    }
}
