package com.possystem.auth.pos;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PosProfileRepository extends JpaRepository<PosProfile, UUID> {

    List<PosProfile> findByShopId(UUID shopId);

    Optional<PosProfile> findByProfileCode(String profileCode);

    Optional<PosProfile> findByProfileIdAndShopId(UUID profileId, UUID shopId);

    List<PosProfile> findByShopIdAndIsActiveTrue(UUID shopId);
}
