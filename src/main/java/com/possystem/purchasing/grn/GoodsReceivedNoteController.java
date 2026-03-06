package com.possystem.purchasing.grn;

import com.possystem.common.ApiResponse;
import com.possystem.common.ListResponse;
import com.possystem.security.RequiresModule;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/grn")
@RequiredArgsConstructor
@RequiresModule("PURCHASING")
public class GoodsReceivedNoteController {

    private final GoodsReceivedNoteService grnService;

    @PostMapping("/save")
    @PreAuthorize("hasAnyRole('SYSTEM_OWNER', 'TENANT_ADMIN') or hasAuthority('GRN_CREATE') or hasAuthority('GRN_EDIT')")
    public ResponseEntity<ApiResponse<GrnResponse>> save(
            @Valid @RequestBody GrnRequest request) {
        GrnResponse response = grnService.save(request);
        String message = request.getId() != null ? "GRN updated" : "GRN created";
        HttpStatus status = request.getId() != null ? HttpStatus.OK : HttpStatus.CREATED;

        if (status == HttpStatus.CREATED) {
            return ResponseEntity.status(status).body(ApiResponse.created(response, message));
        }
        return ResponseEntity.ok(ApiResponse.success(response, message));
    }

    @PostMapping("/fetch")
    @PreAuthorize("hasAnyRole('SYSTEM_OWNER', 'TENANT_ADMIN') or hasAuthority('GRN_VIEW')")
    public ResponseEntity<ListResponse<GrnResponse>> fetch(
            @RequestBody GrnFetchRequest request) {
        ListResponse<GrnResponse> response = grnService.fetch(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/complete")
    @PreAuthorize("hasAnyRole('SYSTEM_OWNER', 'TENANT_ADMIN') or hasAuthority('GRN_EDIT')")
    public ResponseEntity<ApiResponse<GrnResponse>> complete(
            @Valid @RequestBody GrnActionRequest request) {
        GrnResponse response = grnService.completeGrn(request.getId());
        return ResponseEntity.ok(ApiResponse.success(response, "GRN completed. Stock updated."));
    }

    @PostMapping("/cancel")
    @PreAuthorize("hasAnyRole('SYSTEM_OWNER', 'TENANT_ADMIN') or hasAuthority('GRN_EDIT')")
    public ResponseEntity<ApiResponse<GrnResponse>> cancel(
            @Valid @RequestBody GrnActionRequest request) {
        GrnResponse response = grnService.cancelGrn(request.getId());
        return ResponseEntity.ok(ApiResponse.success(response, "GRN cancelled. Stock reversed."));
    }
}
