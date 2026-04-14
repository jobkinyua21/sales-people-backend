package com.salespeople.common.file;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "File storage response")
public class FileStorageResponse {

    @Schema(description = "File ID")
    private UUID fileId;

    @Schema(description = "Stored file name (UUID-based)")
    private String fileName;

    @Schema(description = "Original file name")
    private String originalName;

    @Schema(description = "Storage directory")
    private String directory;

    @Schema(description = "File type/extension")
    private String fileType;

    @Schema(description = "Content type (MIME)")
    private String contentType;

    @Schema(description = "File size in bytes")
    private Long fileSize;

    @Schema(description = "Public URL to access the file")
    private String fileUrl;

    @Schema(description = "Reference ID (e.g., user ID, product ID)")
    private UUID referenceId;

    @Schema(description = "Reference type (e.g., USER, PRODUCT)")
    private String referenceType;

    @Schema(description = "Upload timestamp")
    private LocalDateTime createdAt;
}
