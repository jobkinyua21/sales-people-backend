package com.salespeople.common.file;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileStorageService {

    private final FileStorageRepository fileStorageRepository;

    @Value("${file.upload-dir:uploads}")
    private String uploadDir;

    @Value("${file.base-url:http://localhost:8080/api/v1/public/files}")
    private String baseUrl;

    @Transactional
    public FileStorageResponse uploadFile(MultipartFile file, String directory) {
        return uploadFile(file, directory, null, null);
    }

    @Transactional
    public FileStorageResponse uploadFile(MultipartFile file, String directory, UUID referenceId, String referenceType) {
        try {
            String originalFileName = StringUtils.cleanPath(file.getOriginalFilename());
            String fileExtension = getFileExtension(originalFileName);
            String uniqueFileName = UUID.randomUUID().toString() + fileExtension;

            Path uploadPath = Paths.get(uploadDir, directory);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            Path filePath = uploadPath.resolve(uniqueFileName);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            String fileUrl = baseUrl + "/" + directory + "/" + uniqueFileName;

            FileStorage fileStorage = FileStorage.builder()
                    .fileName(uniqueFileName)
                    .originalName(originalFileName)
                    .directory(directory)
                    .filePath(filePath.toString())
                    .fileType(fileExtension.replace(".", ""))
                    .contentType(file.getContentType())
                    .fileSize(file.getSize())
                    .fileUrl(fileUrl)
                    .referenceId(referenceId)
                    .referenceType(referenceType)
                    .isActive(true)
                    .build();

            FileStorage saved = fileStorageRepository.save(fileStorage);
            log.info("File uploaded: {} -> {}", originalFileName, filePath);

            return mapToResponse(saved);

        } catch (IOException e) {
            log.error("Failed to upload file: {}", e.getMessage());
            throw new RuntimeException("Failed to upload file: " + e.getMessage());
        }
    }

    @Transactional
    public List<FileStorageResponse> uploadMultipleFiles(List<MultipartFile> files, String directory,
                                                          UUID referenceId, String referenceType) {
        return files.stream()
                .map(file -> uploadFile(file, directory, referenceId, referenceType))
                .collect(Collectors.toList());
    }

    public Resource loadFileAsResource(String directory, String fileName) {
        try {
            Path filePath = Paths.get(uploadDir, directory).resolve(fileName).normalize();
            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists() && resource.isReadable()) {
                return resource;
            } else {
                throw new RuntimeException("File not found: " + fileName);
            }
        } catch (MalformedURLException e) {
            throw new RuntimeException("File not found: " + fileName);
        }
    }

    public FileStorageResponse getFile(UUID fileId) {
        FileStorage fileStorage = fileStorageRepository.findByFileIdAndIsActiveTrue(fileId)
                .orElseThrow(() -> new IllegalArgumentException("File not found"));
        return mapToResponse(fileStorage);
    }

    public List<FileStorageResponse> getFilesByReference(UUID referenceId, String referenceType) {
        return fileStorageRepository.findByReferenceIdAndReferenceTypeAndIsActiveTrue(referenceId, referenceType)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public void deleteFile(UUID fileId) {
        FileStorage fileStorage = fileStorageRepository.findById(fileId)
                .orElseThrow(() -> new IllegalArgumentException("File not found"));

        try {
            Path filePath = Paths.get(fileStorage.getFilePath());
            Files.deleteIfExists(filePath);
        } catch (IOException e) {
            log.warn("Could not delete physical file: {}", e.getMessage());
        }

        fileStorage.setIsActive(false);
        fileStorageRepository.save(fileStorage);
        log.info("File deleted: {}", fileStorage.getFileName());
    }

    @Transactional
    public void hardDeleteFile(UUID fileId) {
        FileStorage fileStorage = fileStorageRepository.findById(fileId)
                .orElseThrow(() -> new IllegalArgumentException("File not found"));

        try {
            Path filePath = Paths.get(fileStorage.getFilePath());
            Files.deleteIfExists(filePath);
        } catch (IOException e) {
            log.warn("Could not delete physical file: {}", e.getMessage());
        }

        fileStorageRepository.delete(fileStorage);
        log.info("File hard deleted: {}", fileStorage.getFileName());
    }

    private String getFileExtension(String fileName) {
        if (fileName == null || fileName.lastIndexOf(".") == -1) {
            return "";
        }
        return fileName.substring(fileName.lastIndexOf("."));
    }

    private FileStorageResponse mapToResponse(FileStorage fileStorage) {
        return FileStorageResponse.builder()
                .fileId(fileStorage.getFileId())
                .fileName(fileStorage.getFileName())
                .originalName(fileStorage.getOriginalName())
                .directory(fileStorage.getDirectory())
                .fileType(fileStorage.getFileType())
                .contentType(fileStorage.getContentType())
                .fileSize(fileStorage.getFileSize())
                .fileUrl(fileStorage.getFileUrl())
                .referenceId(fileStorage.getReferenceId())
                .referenceType(fileStorage.getReferenceType())
                .createdAt(fileStorage.getCreatedAt())
                .build();
    }
}
