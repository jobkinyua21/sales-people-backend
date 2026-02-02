package com.possystem.generalsetting.packages;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PackageModuleRepository extends JpaRepository<PackageModule, UUID> {

    List<PackageModule> findByPosPackage_PackageId(UUID packageId);

    List<PackageModule> findByPosModule_ModuleId(UUID moduleId);

    Optional<PackageModule> findByPosPackage_PackageIdAndPosModule_ModuleId(UUID packageId, UUID moduleId);

    List<PackageModule> findByPosPackage_PackageIdAndIsIncludedTrue(UUID packageId);

    List<PackageModule> findByPosPackage_PackageIdAndIsIncludedFalse(UUID packageId);
}
