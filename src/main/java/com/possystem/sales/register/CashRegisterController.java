package com.possystem.sales.register;

import com.possystem.common.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/cash-register")
@RequiredArgsConstructor
public class CashRegisterController {

    private final CashRegisterService cashRegisterService;

    @PostMapping("/open")
    @PreAuthorize("hasAnyRole('SYSTEM_OWNER', 'TENANT_ADMIN') or hasAuthority('CASH_REGISTER_MANAGE')")
    public ResponseEntity<ApiResponse<CashRegisterSessionResponse>> open(@Valid @RequestBody OpenRegisterRequest request) {
        CashRegisterSessionResponse response = cashRegisterService.openRegister(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(response, "Register opened"));
    }

    @PostMapping("/close")
    @PreAuthorize("hasAnyRole('SYSTEM_OWNER', 'TENANT_ADMIN') or hasAuthority('CASH_REGISTER_MANAGE')")
    public ResponseEntity<ApiResponse<CashRegisterSessionResponse>> close(@Valid @RequestBody CloseRegisterRequest request) {
        CashRegisterSessionResponse response = cashRegisterService.closeRegister(request);
        return ResponseEntity.ok(ApiResponse.success(response, "Register closed"));
    }

    @PostMapping("/movement")
    @PreAuthorize("hasAnyRole('SYSTEM_OWNER', 'TENANT_ADMIN') or hasAuthority('CASH_REGISTER_MANAGE')")
    public ResponseEntity<ApiResponse<CashRegisterSessionResponse>> addMovement(@Valid @RequestBody CashMovementRequest request) {
        CashRegisterSessionResponse response = cashRegisterService.addCashMovement(request);
        return ResponseEntity.ok(ApiResponse.success(response, "Cash movement recorded"));
    }

    @PostMapping("/current")
    @PreAuthorize("hasAnyRole('SYSTEM_OWNER', 'TENANT_ADMIN') or hasAuthority('CASH_REGISTER_VIEW') or hasAuthority('CASH_REGISTER_MANAGE')")
    public ResponseEntity<ApiResponse<CashRegisterSessionResponse>> current() {
        CashRegisterSessionResponse response = cashRegisterService.getCurrentSession();
        if (response == null) {
            return ResponseEntity.ok(ApiResponse.success(null, "No open register session"));
        }
        return ResponseEntity.ok(ApiResponse.success(response, "Current session"));
    }

    @PostMapping("/my-history")
    @PreAuthorize("hasAnyRole('SYSTEM_OWNER', 'TENANT_ADMIN') or hasAuthority('CASH_REGISTER_VIEW') or hasAuthority('CASH_REGISTER_MANAGE')")
    public ResponseEntity<ApiResponse<List<CashRegisterSessionResponse>>> myHistory() {
        List<CashRegisterSessionResponse> sessions = cashRegisterService.getMySessionHistory();
        return ResponseEntity.ok(ApiResponse.success(sessions, "My session history"));
    }

    @PostMapping("/history")
    @PreAuthorize("hasAnyRole('SYSTEM_OWNER', 'TENANT_ADMIN') or hasAuthority('CASH_REGISTER_VIEW')")
    public ResponseEntity<ApiResponse<List<CashRegisterSessionResponse>>> allHistory() {
        List<CashRegisterSessionResponse> sessions = cashRegisterService.getAllSessionHistory();
        return ResponseEntity.ok(ApiResponse.success(sessions, "All session history"));
    }
}
