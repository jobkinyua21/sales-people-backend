package com.possystem.businesstype;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BusinessTypeRepository extends JpaRepository<BusinessType, UUID> {

    long count();

    boolean existsByCode(String code);

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, UUID id);

    Optional<BusinessType> findByIdAndIsActiveTrue(UUID id);

    @Query("SELECT bt FROM BusinessType bt WHERE bt.isActive = true AND " +
            "(:search IS NULL OR :search = '' OR " +
            "LOWER(bt.code) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(bt.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(bt.description) LIKE LOWER(CONCAT('%', :search, '%'))) " +
            "ORDER BY bt.name ASC")
    Page<BusinessType> searchAll(@Param("search") String search, Pageable pageable);

    @Query("SELECT bt FROM BusinessType bt WHERE bt.isActive = true AND " +
            "(:search IS NULL OR :search = '' OR " +
            "LOWER(bt.code) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(bt.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(bt.description) LIKE LOWER(CONCAT('%', :search, '%'))) " +
            "ORDER BY bt.name ASC")
    List<BusinessType> searchAll(@Param("search") String search);
}
