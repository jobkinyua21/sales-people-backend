package com.possystem.common;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/public/error-codes")
@Tag(name = "Error Codes", description = "Reference endpoint for all API error codes")
public class ErrorCodeController {

    @Operation(
            summary = "Get all error codes",
            description = "Returns all available error codes with their descriptions. " +
                    "Use this to build error handling logic in the frontend."
    )
    @GetMapping
    public ResponseEntity<ApiResponse<List<ErrorCodeInfo>>> getAllErrorCodes() {
        List<ErrorCodeInfo> errorCodes = Arrays.stream(ErrorCode.values())
                .map(e -> new ErrorCodeInfo(
                        e.getCode(),
                        e.name(),
                        e.getDefaultMessage(),
                        getHttpStatus(e),
                        getCategory(e)
                ))
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success(errorCodes));
    }

    @Operation(
            summary = "Get error codes grouped by category",
            description = "Returns error codes grouped by their category (AUTH, AUTHZ, VAL, etc.)"
    )
    @GetMapping("/grouped")
    public ResponseEntity<ApiResponse<Map<String, List<ErrorCodeInfo>>>> getErrorCodesGrouped() {
        Map<String, List<ErrorCodeInfo>> grouped = Arrays.stream(ErrorCode.values())
                .map(e -> new ErrorCodeInfo(
                        e.getCode(),
                        e.name(),
                        e.getDefaultMessage(),
                        getHttpStatus(e),
                        getCategory(e)
                ))
                .collect(Collectors.groupingBy(ErrorCodeInfo::category));

        return ResponseEntity.ok(ApiResponse.success(grouped));
    }

    private int getHttpStatus(ErrorCode errorCode) {
        String code = errorCode.getCode();
        if (code.startsWith("AUTH_")) return 401;
        if (code.startsWith("AUTHZ_")) return 403;
        if (code.startsWith("VAL_")) return 400;
        if (code.startsWith("CONF_")) return 409;
        if (code.startsWith("NF_")) return 404;
        if (code.startsWith("SRV_")) return 500;
        return 500;
    }

    private String getCategory(ErrorCode errorCode) {
        String code = errorCode.getCode();
        if (code.startsWith("AUTH_")) return "AUTHENTICATION";
        if (code.startsWith("AUTHZ_")) return "AUTHORIZATION";
        if (code.startsWith("VAL_")) return "VALIDATION";
        if (code.startsWith("CONF_")) return "CONFLICT";
        if (code.startsWith("NF_")) return "NOT_FOUND";
        if (code.startsWith("SRV_")) return "SERVER";
        return "OTHER";
    }

    public record ErrorCodeInfo(
            String code,
            String name,
            String message,
            int httpStatus,
            String category
    ) {}
}
