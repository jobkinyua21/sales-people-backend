package com.salespeople.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Standard API response wrapper")
public class ApiResponse<T> {

    @Schema(description = "Indicates if the request was successful", example = "true")
    private boolean success;

    @Schema(description = "Response message", example = "Operation completed successfully")
    private String message;

    @Schema(description = "Response data containing result")
    private Map<String, T> data;

    @Schema(description = "HTTP status code", example = "200")
    private int status;

    @Schema(description = "Response timestamp")
    private LocalDateTime timestamp;

    @Schema(description = "Error code for frontend handling (only present on errors)", example = "AUTH_001")
    private String errorCode;

    @Schema(description = "Error details (only present on validation errors)")
    private Object errors;

    private static <T> Map<String, T> wrapResult(T result) {
        if (result == null) return null;
        return Map.of("result", result);
    }

    // Success responses
    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .message("Operation completed successfully")
                .data(wrapResult(data))
                .status(200)
                .timestamp(LocalDateTime.now())
                .build();
    }

    public static <T> ApiResponse<T> success(T data, String message) {
        return ApiResponse.<T>builder()
                .success(true)
                .message(message)
                .data(wrapResult(data))
                .status(200)
                .timestamp(LocalDateTime.now())
                .build();
    }

    public static <T> ApiResponse<T> success(T data, String message, int status) {
        return ApiResponse.<T>builder()
                .success(true)
                .message(message)
                .data(wrapResult(data))
                .status(status)
                .timestamp(LocalDateTime.now())
                .build();
    }

    public static <T> ApiResponse<T> created(T data, String message) {
        return ApiResponse.<T>builder()
                .success(true)
                .message(message)
                .data(wrapResult(data))
                .status(201)
                .timestamp(LocalDateTime.now())
                .build();
    }

    // Error responses with ErrorCode enum
    public static <T> ApiResponse<T> error(ErrorCode errorCode, int status) {
        return ApiResponse.<T>builder()
                .success(false)
                .message(errorCode.getDefaultMessage())
                .errorCode(errorCode.getCode())
                .status(status)
                .timestamp(LocalDateTime.now())
                .build();
    }

    public static <T> ApiResponse<T> error(ErrorCode errorCode, String message, int status) {
        return ApiResponse.<T>builder()
                .success(false)
                .message(message)
                .errorCode(errorCode.getCode())
                .status(status)
                .timestamp(LocalDateTime.now())
                .build();
    }

    public static <T> ApiResponse<T> error(ErrorCode errorCode, int status, Object errors) {
        return ApiResponse.<T>builder()
                .success(false)
                .message(errorCode.getDefaultMessage())
                .errorCode(errorCode.getCode())
                .status(status)
                .errors(errors)
                .timestamp(LocalDateTime.now())
                .build();
    }

    // Legacy error methods (without ErrorCode)
    public static <T> ApiResponse<T> error(String message, int status) {
        return ApiResponse.<T>builder()
                .success(false)
                .message(message)
                .status(status)
                .timestamp(LocalDateTime.now())
                .build();
    }

    public static <T> ApiResponse<T> error(String message, int status, Object errors) {
        return ApiResponse.<T>builder()
                .success(false)
                .message(message)
                .status(status)
                .errors(errors)
                .timestamp(LocalDateTime.now())
                .build();
    }
}
