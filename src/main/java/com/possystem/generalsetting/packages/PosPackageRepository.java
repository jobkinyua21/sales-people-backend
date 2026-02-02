package com.possystem.generalsetting.packages;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PosPackageRepository extends JpaRepository<PosPackage, UUID> {

    Optional<PosPackage> findByPackageCode(String packageCode);

    List<PosPackage> findByIsActiveTrue();

    boolean existsByPackageCode(String packageCode);
}
