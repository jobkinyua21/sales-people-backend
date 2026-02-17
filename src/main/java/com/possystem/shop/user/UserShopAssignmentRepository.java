package com.possystem.shop;

import com.possystem.common.UserType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserShopAssignmentRepository extends JpaRepository<UserShopAssignment, UUID> {

    List<UserShopAssignment> findByUserIdAndIsActiveTrue(UUID userId);

    List<UserShopAssignment> findByShopIdAndIsActiveTrue(UUID shopId);

    Optional<UserShopAssignment> findByUserIdAndShopIdAndIsActiveTrue(UUID userId, UUID shopId);

    Optional<UserShopAssignment> findByUserIdAndShopId(UUID userId, UUID shopId);

    boolean existsByUserIdAndShopIdAndIsActiveTrue(UUID userId, UUID shopId);

    List<UserShopAssignment> findByShopIdAndShopRoleAndIsActiveTrue(UUID shopId, UserType shopRole);

    long countByUserIdAndIsActiveTrue(UUID userId);

    long countByShopIdAndIsActiveTrue(UUID shopId);
}
