package com.salespeople.item;

import com.salespeople.common.ApiResponse;
import com.salespeople.common.ListResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/items")
@RequiredArgsConstructor
public class ItemsRegisterController {

    private final ItemsRegisterService itemsRegisterService;

    @PostMapping("/fetch")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ListResponse<ItemsRegisterResponse>> fetch(@RequestBody ItemsFetchRequest request) {
        return ResponseEntity.ok(itemsRegisterService.fetch(request));
    }

    @PostMapping("/toggle-disabled/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ItemsRegisterResponse>> toggleDisabled(@PathVariable Long id) {
        ItemsRegisterResponse response = itemsRegisterService.toggleDisabled(id);
        return ResponseEntity.ok(ApiResponse.success(response, "Item status updated"));
    }
}
