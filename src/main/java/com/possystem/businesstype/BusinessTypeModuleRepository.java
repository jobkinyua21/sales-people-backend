package com.possystem.businesstype;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface BusinessTypeModuleRepository extends JpaRepository<BusinessTypeModule, UUID> {

    List<BusinessTypeModule> findByBusinessTypeId(UUID businessTypeId);

    List<BusinessTypeModule> findByBusinessTypeIdAndIsDefaultTrue(UUID businessTypeId);

    List<BusinessTypeModule> findByBusinessTypeIdAndIsDefaultFalse(UUID businessTypeId);

    void deleteByBusinessTypeId(UUID businessTypeId);

    @Query("SELECT COUNT(btm) > 0 FROM BusinessTypeModule btm " +
           "JOIN com.possystem.module.AdditionalModule am ON btm.additionalModuleId = am.id " +
           "WHERE btm.businessTypeId = :businessTypeId AND am.moduleCode = :moduleCode AND btm.isDefault = true")
    boolean existsDefaultModuleByBusinessTypeIdAndModuleCode(@Param("businessTypeId") UUID businessTypeId, @Param("moduleCode") String moduleCode);
}
