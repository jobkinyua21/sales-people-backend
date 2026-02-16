package com.possystem.menu;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface MenuRepository extends JpaRepository<Menu, UUID> {

    List<Menu> findByIsActiveTrueOrderBySortOrder();

    boolean existsByMenuCode(String menuCode);
}
