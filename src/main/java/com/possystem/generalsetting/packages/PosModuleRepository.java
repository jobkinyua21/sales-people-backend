package com.possystem.generalsetting.packages;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PosModuleRepository extends JpaRepository<PosModule, UUID> {

    Optional<PosModule> findByModuleCode(String moduleCode);

    List<PosModule> findByIsActiveTrue();

    List<PosModule> findByIsCoreTrue();

    boolean existsByModuleCode(String moduleCode);
}
