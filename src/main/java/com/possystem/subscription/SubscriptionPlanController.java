package com.possystem.subscription;

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
@RequestMapping("/api/v1/subscription-plans")
@RequiredArgsConstructor
public class SubscriptionPlanController {

    private final SubscriptionPlanService subscriptionPlanService;

    @PostMapping("/save")
    public ResponseEntity<ApiResponse<SubscriptionPlanResponse>> save(
            @Valid @RequestBody SubscriptionPlanRequest request) {
        SubscriptionPlanResponse response = subscriptionPlanService.save(request);
        String message = request.getId() != null ? "Subscription plan updated" : "Subscription plan created";
        HttpStatus status = request.getId() != null ? HttpStatus.OK : HttpStatus.CREATED;

        if (status == HttpStatus.CREATED) {
            return ResponseEntity.status(status).body(ApiResponse.created(response, message));
        }
        return ResponseEntity.ok(ApiResponse.success(response, message));
    }

    @PostMapping("/fetch")
    public ResponseEntity<ListResponse<SubscriptionPlanResponse>> fetch(@RequestBody FetchRequest request) {
        ListResponse<SubscriptionPlanResponse> response = subscriptionPlanService.fetch(request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        subscriptionPlanService.delete(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Subscription plan deleted"));
    }
}
