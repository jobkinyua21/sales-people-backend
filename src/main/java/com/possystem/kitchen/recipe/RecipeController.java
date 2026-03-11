package com.possystem.kitchen.recipe;

import com.possystem.common.ApiResponse;
import com.possystem.security.RequiresModule;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/recipes")
@RequiredArgsConstructor
@RequiresModule("KITCHEN")
public class RecipeController {

    private final RecipeService recipeService;

    @PostMapping("/save")
    @PreAuthorize("hasAnyRole('SYSTEM_OWNER', 'TENANT_ADMIN') or hasAuthority('RECIPES_CREATE') or hasAuthority('RECIPES_EDIT')")
    public ResponseEntity<ApiResponse<RecipeResponse>> save(@Valid @RequestBody RecipeRequest request) {
        RecipeResponse response = recipeService.save(request);
        return ResponseEntity.status(request.getId() != null ? HttpStatus.OK : HttpStatus.CREATED)
                .body(request.getId() != null
                        ? ApiResponse.success(response, "Recipe updated")
                        : ApiResponse.created(response, "Recipe created"));
    }

    @PostMapping("/fetch")
    @PreAuthorize("hasAnyRole('SYSTEM_OWNER', 'TENANT_ADMIN') or hasAuthority('RECIPES_VIEW')")
    public ResponseEntity<ApiResponse<List<RecipeResponse>>> fetch(@RequestBody RecipeFetchRequest request) {
        List<RecipeResponse> recipes = recipeService.fetch(request);
        return ResponseEntity.ok(ApiResponse.success(recipes, "Recipes fetched"));
    }

    @PostMapping("/activate/{id}")
    @PreAuthorize("hasAnyRole('SYSTEM_OWNER', 'TENANT_ADMIN') or hasAuthority('RECIPES_EDIT')")
    public ResponseEntity<ApiResponse<RecipeResponse>> activate(@PathVariable UUID id) {
        RecipeResponse response = recipeService.activate(id);
        return ResponseEntity.ok(ApiResponse.success(response, "Recipe activated"));
    }

    @PostMapping("/deactivate/{id}")
    @PreAuthorize("hasAnyRole('SYSTEM_OWNER', 'TENANT_ADMIN') or hasAuthority('RECIPES_EDIT')")
    public ResponseEntity<ApiResponse<RecipeResponse>> deactivate(@PathVariable UUID id) {
        RecipeResponse response = recipeService.deactivate(id);
        return ResponseEntity.ok(ApiResponse.success(response, "Recipe deactivated"));
    }

    @PostMapping("/prep-sheet/{id}")
    @PreAuthorize("hasAnyRole('SYSTEM_OWNER', 'TENANT_ADMIN') or hasAuthority('RECIPES_VIEW') or hasAuthority('KITCHEN_ORDERS_MANAGE')")
    public ResponseEntity<ApiResponse<PrepSheetResponse>> prepSheet(
            @PathVariable UUID id,
            @RequestParam BigDecimal quantity) {
        PrepSheetResponse response = recipeService.getPrepSheet(id, quantity);
        return ResponseEntity.ok(ApiResponse.success(response, "Prep sheet generated"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SYSTEM_OWNER', 'TENANT_ADMIN') or hasAuthority('RECIPES_DELETE')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        recipeService.delete(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Recipe deleted"));
    }
}
