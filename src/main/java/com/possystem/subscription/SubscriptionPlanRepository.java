package com.possystem.subscription;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SubscriptionPlanRepository extends JpaRepository<SubscriptionPlan, UUID> {

    boolean existsByPlanCode(String planCode);

    @Query("SELECT sp FROM SubscriptionPlan sp WHERE " +
            "(:search IS NULL OR :search = '' OR " +
            "LOWER(sp.planCode) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(sp.planName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(sp.planType) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(sp.billingLevel) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(CAST(sp.status AS string)) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(sp.currency) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(sp.description) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(sp.reportLevel) LIKE LOWER(CONCAT('%', :search, '%'))) " +
            "ORDER BY sp.createdAt DESC")
    Page<SubscriptionPlan> searchAll(@Param("search") String search, Pageable pageable);

    @Query("SELECT sp FROM SubscriptionPlan sp WHERE " +
            "(:search IS NULL OR :search = '' OR " +
            "LOWER(sp.planCode) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(sp.planName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(sp.planType) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(sp.billingLevel) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(CAST(sp.status AS string)) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(sp.currency) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(sp.description) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(sp.reportLevel) LIKE LOWER(CONCAT('%', :search, '%'))) " +
            "ORDER BY sp.createdAt DESC")
    List<SubscriptionPlan> searchAll(@Param("search") String search);
}
