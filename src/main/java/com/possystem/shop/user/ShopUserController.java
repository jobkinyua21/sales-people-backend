package com.possystem.shop;

import com.possystem.common.ApiResponse;
import com.possystem.common.FetchRequest;
import com.possystem.common.ListResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/shop-users")
@RequiredArgsConstructor
public class ShopUserController {

    private final ShopUserService shopUserService;

    @PostMapping("/save")
    @PreAuthorize("hasAnyRole('SYSTEM_OWNER', 'TENANT_ADMIN') or hasAuthority('USERS_CREATE') or hasAuthority('USERS_EDIT')")
    public ResponseEntity<ApiResponse<ShopUserResponse>> save(@Valid @RequestBody ShopUserRequest request) {
        ShopUserResponse response = shopUserService.save(request);
        String message = request.getId() != null ? "User updated" : "User created";
        HttpStatus status = request.getId() != null ? HttpStatus.OK : HttpStatus.CREATED;

        if (status == HttpStatus.CREATED) {
            return ResponseEntity.status(status).body(ApiResponse.created(response, message));
        }
        return ResponseEntity.ok(ApiResponse.success(response, message));
    }

    @PostMapping("/fetch")
    @PreAuthorize("hasAnyRole('SYSTEM_OWNER', 'TENANT_ADMIN') or hasAuthority('USERS_VIEW')")
    public ResponseEntity<ListResponse<ShopUserResponse>> fetch(@RequestBody FetchRequest request) {
        ListResponse<ShopUserResponse> response = shopUserService.fetch(request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SYSTEM_OWNER', 'TENANT_ADMIN') or hasAuthority('USERS_DELETE')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        shopUserService.delete(id);
        return ResponseEntity.ok(ApiResponse.success(null, "User deleted"));
    }
}
