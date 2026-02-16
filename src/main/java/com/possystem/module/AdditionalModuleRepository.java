package com.possystem.module;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AdditionalModuleRepository extends JpaRepository<AdditionalModule, UUID> {

    boolean existsByModuleCode(String moduleCode);

    @Query("SELECT m FROM AdditionalModule m WHERE " +
            "(:search IS NULL OR :search = '' OR " +
            "LOWER(m.moduleCode) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(m.moduleName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(m.description) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(CAST(m.status AS string)) LIKE LOWER(CONCAT('%', :search, '%'))) " +
            "ORDER BY m.createdAt DESC")
    Page<AdditionalModule> searchAll(@Param("search") String search, Pageable pageable);

    @Query("SELECT m FROM AdditionalModule m WHERE " +
            "(:search IS NULL OR :search = '' OR " +
            "LOWER(m.moduleCode) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(m.moduleName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(m.description) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(CAST(m.status AS string)) LIKE LOWER(CONCAT('%', :search, '%'))) " +
            "ORDER BY m.createdAt DESC")
    List<AdditionalModule> searchAll(@Param("search") String search);
}
