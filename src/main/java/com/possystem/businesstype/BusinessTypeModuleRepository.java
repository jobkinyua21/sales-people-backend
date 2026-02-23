package com.possystem.businesstype;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface BusinessTypeModuleRepository extends JpaRepository<BusinessTypeModule, UUID> {

    List<BusinessTypeModule> findByBusinessTypeId(UUID businessTypeId);

    List<BusinessTypeModule> findByBusinessTypeIdAndIsDefaultTrue(UUID businessTypeId);

    List<BusinessTypeModule> findByBusinessTypeIdAndIsDefaultFalse(UUID businessTypeId);

    void deleteByBusinessTypeId(UUID businessTypeId);
}
