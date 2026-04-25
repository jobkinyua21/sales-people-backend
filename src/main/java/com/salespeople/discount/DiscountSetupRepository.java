package com.salespeople.discount;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DiscountSetupRepository extends JpaRepository<DiscountSetup, Long> {

    Optional<DiscountSetup> findByItemCode(Integer itemCode);

    boolean existsByItemCode(Integer itemCode);
}
