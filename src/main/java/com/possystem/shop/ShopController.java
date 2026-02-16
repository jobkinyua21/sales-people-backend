package com.possystem.shop;

import com.possystem.common.ApiResponse;
import com.possystem.common.FetchRequest;
import com.possystem.common.ListResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/shops")
@RequiredArgsConstructor
public class ShopController {

    private final ShopService shopService;

    @PostMapping("/save")
    public ResponseEntity<ApiResponse<ShopResponse>> save(@Valid @RequestBody ShopRequest request) {
        ShopResponse response = shopService.save(request);
        String message = request.getId() != null ? "Shop updated" : "Shop created";
        HttpStatus status = request.getId() != null ? HttpStatus.OK : HttpStatus.CREATED;

        if (status == HttpStatus.CREATED) {
            return ResponseEntity.status(status).body(ApiResponse.created(response, message));
        }
        return ResponseEntity.ok(ApiResponse.success(response, message));
    }

    @PostMapping("/fetch")
    public ResponseEntity<ListResponse<ShopResponse>> fetch(@RequestBody FetchRequest request) {
        ListResponse<ShopResponse> response = shopService.fetch(request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        shopService.delete(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Shop deleted"));
    }
}
