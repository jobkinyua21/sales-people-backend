package com.possystem.generalsetting.users;

import com.possystem.common.ApiResponse;
import com.possystem.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/tenant/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('TENANT')")
@Tag(name = "Shop Users", description = "Manage POS users for tenant shops")
@SecurityRequirement(name = "Bearer Authentication")
public class ShopUserController {

    private final ShopUserService shopUserService;

    @Operation(summary = "Create shop user", description = "Create a new POS user for a shop")
    @PostMapping
    public ResponseEntity<ApiResponse<ShopUserResponse>> createUser(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CreateShopUserRequest request) {
        ShopUserResponse response = shopUserService.createUser(principal.getId(), request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.created(response, "User created successfully"));
    }

    @Operation(summary = "Update shop user", description = "Update an existing POS user")
    @PutMapping("/{userId}")
    public ResponseEntity<ApiResponse<ShopUserResponse>> updateUser(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID userId,
            @Valid @RequestBody UpdateShopUserRequest request) {
        ShopUserResponse response = shopUserService.updateUser(principal.getId(), userId, request);
        return ResponseEntity.ok(ApiResponse.success(response, "User updated successfully"));
    }

    @Operation(summary = "Get shop user", description = "Get a specific POS user by ID")
    @GetMapping("/{userId}")
    public ResponseEntity<ApiResponse<ShopUserResponse>> getUser(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID userId) {
        ShopUserResponse response = shopUserService.getUser(principal.getId(), userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "Get all shop users", description = "Get all POS users for the tenant")
    @GetMapping
    public ResponseEntity<ApiResponse<List<ShopUserResponse>>> getAllUsers(
            @AuthenticationPrincipal UserPrincipal principal) {
        List<ShopUserResponse> response = shopUserService.getUsersByTenant(principal.getId());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "Get users by shop", description = "Get all POS users for a specific shop")
    @GetMapping("/shop/{shopId}")
    public ResponseEntity<ApiResponse<List<ShopUserResponse>>> getUsersByShop(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID shopId) {
        List<ShopUserResponse> response = shopUserService.getUsersByShop(principal.getId(), shopId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "Delete shop user", description = "Delete a POS user")
    @DeleteMapping("/{userId}")
    public ResponseEntity<ApiResponse<Void>> deleteUser(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID userId) {
        shopUserService.deleteUser(principal.getId(), userId);
        return ResponseEntity.ok(ApiResponse.success(null, "User deleted successfully"));
    }
}
