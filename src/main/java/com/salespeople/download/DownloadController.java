package com.salespeople.download;

import com.salespeople.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
@RequestMapping("/api/download")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(originPatterns = "*", allowCredentials = "true")
public class DownloadController {

    private static final String DOWNLOAD_DIRECTORY = "/var/www/download";
    private static final String APK_FILENAME = "sales-people.apk";

    @GetMapping("/apk")
    public ResponseEntity<Resource> downloadApk() {
        try {
            Path filePath = Paths.get(DOWNLOAD_DIRECTORY, APK_FILENAME);
            File file = filePath.toFile();

            if (!file.exists()) {
                log.error("APK file not found at: {}", filePath);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }

            Resource resource = new FileSystemResource(file);

            String contentType = Files.probeContentType(filePath);
            if (contentType == null) {
                contentType = "application/vnd.android.package-archive";
            }

            log.info("Serving APK: {} ({} bytes)", APK_FILENAME, file.length());

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + APK_FILENAME + "\"")
                    .contentLength(file.length())
                    .body(resource);

        } catch (Exception e) {
            log.error("Error downloading APK", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/apk/info")
    public ResponseEntity<ApiResponse<ApkInfo>> getApkInfo() {
        try {
            Path filePath = Paths.get(DOWNLOAD_DIRECTORY, APK_FILENAME);
            File file = filePath.toFile();

            if (!file.exists()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error("APK file not found", 404));
            }

            ApkInfo info = ApkInfo.builder()
                    .filename(APK_FILENAME)
                    .size(file.length())
                    .lastModified(file.lastModified())
                    .downloadUrl("/api/download/apk")
                    .build();

            return ResponseEntity.ok(ApiResponse.success(info, "APK info retrieved"));

        } catch (Exception e) {
            log.error("Error retrieving APK info", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Error retrieving APK info", 500));
        }
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class ApkInfo {
        private String filename;
        private long size;
        private long lastModified;
        private String downloadUrl;
    }
}
