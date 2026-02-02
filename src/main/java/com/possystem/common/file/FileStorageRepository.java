package com.possystem.common.file;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FileStorageRepository extends JpaRepository<FileStorage, UUID> {

    Optional<FileStorage> findByFileName(String fileName);

    Optional<FileStorage> findByFileIdAndIsActiveTrue(UUID fileId);

    List<FileStorage> findByReferenceIdAndReferenceType(UUID referenceId, String referenceType);

    List<FileStorage> findByDirectory(String directory);

    List<FileStorage> findByReferenceIdAndReferenceTypeAndIsActiveTrue(UUID referenceId, String referenceType);
}
