package com.possystem.subscription;

import com.possystem.common.ApiResponse;
import com.possystem.common.FetchRequest;
import com.possystem.common.ListResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
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

    @Operation(summary = "Create or update a subscription plan")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(content = @Content(examples = {
            @ExampleObject(name = "Create", value = """
                    {
                      "planName": "Basic Plan",
                      "planType": "BASIC",
                      "billingLevel": "SHOP",
                      "priceMonthly": 1500.00,
                      "priceYearly": 15000.00,
                      "currency": "KES",
                      "maxUsers": 5
                    }
                    """),
            @ExampleObject(name = "Update", value = """
                    {
                      "id": "paste-uuid-here",
                      "planName": "Basic Plan Updated",
                      "planType": "BASIC",
                      "billingLevel": "SHOP",
                      "priceMonthly": 2000.00,
                      "priceYearly": 20000.00,
                      "status": "INACTIVE"
                    }
                    """)
    }))
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

    @Operation(summary = "Fetch subscription plans (paginated or by id)")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(content = @Content(examples = {
            @ExampleObject(name = "Fetch all (no pagination)", value = """
                    {}
                    """),
            @ExampleObject(name = "Paginated", value = """
                    {
                      "start": 0,
                      "limit": 10
                    }
                    """),
            @ExampleObject(name = "Search", value = """
                    {
                      "search": "basic",
                      "start": 0,
                      "limit": 10
                    }
                    """),
            @ExampleObject(name = "Fetch by ID", value = """
                    {
                      "id": "paste-uuid-here"
                    }
                    """)
    }))
    @PostMapping("/fetch")
    public ResponseEntity<?> fetch(@RequestBody FetchRequest request) {
        if (request.getId() != null) {
            SubscriptionPlanResponse response = subscriptionPlanService.getById(request.getId());
            return ResponseEntity.ok(ApiResponse.success(response, "Subscription plan fetched"));
        }

        ListResponse<SubscriptionPlanResponse> response = subscriptionPlanService.fetch(
                request.getStart(), request.getLimit(), request.getSearch());
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        subscriptionPlanService.delete(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Subscription plan deleted"));
    }
}
