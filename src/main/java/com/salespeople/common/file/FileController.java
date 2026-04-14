package com.salespeople.common.file;

import com.salespeople.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@Tag(name = "File Management", description = "Upload and manage files")
public class FileController {

    private final FileStorageService fileStorageService;

    @Operation(summary = "Upload a file", description = "Upload a file to specified directory")
    @SecurityRequirement(name = "Bearer Authentication")
    @PostMapping(value = "/api/v1/files/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<FileStorageResponse>> uploadFile(
            @Parameter(description = "File to upload") @RequestParam("file") MultipartFile file,
            @Parameter(description = "Directory to store file") @RequestParam("directory") String directory,
            @Parameter(description = "Reference ID (optional)") @RequestParam(value = "referenceId", required = false) UUID referenceId,
            @Parameter(description = "Reference type (optional)") @RequestParam(value = "referenceType", required = false) String referenceType) {

        FileStorageResponse response = fileStorageService.uploadFile(file, directory, referenceId, referenceType);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.created(response, "File uploaded successfully"));
    }

    @Operation(summary = "Upload multiple files", description = "Upload multiple files to specified directory")
    @SecurityRequirement(name = "Bearer Authentication")
    @PostMapping(value = "/api/v1/files/upload-multiple", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<List<FileStorageResponse>>> uploadMultipleFiles(
            @Parameter(description = "Files to upload") @RequestParam("files") List<MultipartFile> files,
            @Parameter(description = "Directory to store files") @RequestParam("directory") String directory,
            @Parameter(description = "Reference ID (optional)") @RequestParam(value = "referenceId", required = false) UUID referenceId,
            @Parameter(description = "Reference type (optional)") @RequestParam(value = "referenceType", required = false) String referenceType) {

        List<FileStorageResponse> response = fileStorageService.uploadMultipleFiles(files, directory, referenceId, referenceType);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.created(response, "Files uploaded successfully"));
    }

    @Operation(summary = "Get file info", description = "Get file information by ID")
    @SecurityRequirement(name = "Bearer Authentication")
    @GetMapping("/api/v1/files/{fileId}")
    public ResponseEntity<ApiResponse<FileStorageResponse>> getFileInfo(@PathVariable UUID fileId) {
        FileStorageResponse response = fileStorageService.getFile(fileId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "Get files by reference", description = "Get all files associated with a reference")
    @SecurityRequirement(name = "Bearer Authentication")
    @GetMapping("/api/v1/files/reference/{referenceId}")
    public ResponseEntity<ApiResponse<List<FileStorageResponse>>> getFilesByReference(
            @PathVariable UUID referenceId,
            @RequestParam String referenceType) {
        List<FileStorageResponse> response = fileStorageService.getFilesByReference(referenceId, referenceType);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "Delete file", description = "Soft delete a file")
    @SecurityRequirement(name = "Bearer Authentication")
    @DeleteMapping("/api/v1/files/{fileId}")
    public ResponseEntity<ApiResponse<Void>> deleteFile(@PathVariable UUID fileId) {
        fileStorageService.deleteFile(fileId);
        return ResponseEntity.ok(ApiResponse.success(null, "File deleted successfully"));
    }

    @Operation(summary = "Download/View file", description = "Access file by directory and filename (public)")
    @GetMapping("/api/v1/public/files/{directory}/{fileName}")
    public ResponseEntity<Resource> downloadFile(
            @PathVariable String directory,
            @PathVariable String fileName) {

        Resource resource = fileStorageService.loadFileAsResource(directory, fileName);

        String contentType = "application/octet-stream";
        if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg")) {
            contentType = "image/jpeg";
        } else if (fileName.endsWith(".png")) {
            contentType = "image/png";
        } else if (fileName.endsWith(".gif")) {
            contentType = "image/gif";
        } else if (fileName.endsWith(".pdf")) {
            contentType = "application/pdf";
        } else if (fileName.endsWith(".doc") || fileName.endsWith(".docx")) {
            contentType = "application/msword";
        } else if (fileName.endsWith(".xls") || fileName.endsWith(".xlsx")) {
            contentType = "application/vnd.ms-excel";
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + fileName + "\"")
                .body(resource);
    }
}
