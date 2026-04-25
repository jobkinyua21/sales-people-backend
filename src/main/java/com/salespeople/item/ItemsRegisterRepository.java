package com.salespeople.item;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ItemsRegisterRepository extends JpaRepository<ItemsRegister, Long> {

    Optional<ItemsRegister> findByItemCode(Integer itemCode);

    boolean existsByItemCode(Integer itemCode);

    @Query("""
            SELECT i FROM ItemsRegister i
            WHERE i.disabled = false
            AND (COALESCE(:search, '') = ''
                 OR LOWER(i.itemName) LIKE LOWER(CONCAT('%', :search, '%'))
                 OR CAST(i.itemCode AS string) LIKE CONCAT('%', :search, '%'))
            ORDER BY i.itemName ASC
            """)
    List<ItemsRegister> searchAll(@Param("search") String search);

    @Query("""
            SELECT i FROM ItemsRegister i
            WHERE i.disabled = false
            AND (COALESCE(:search, '') = ''
                 OR LOWER(i.itemName) LIKE LOWER(CONCAT('%', :search, '%'))
                 OR CAST(i.itemCode AS string) LIKE CONCAT('%', :search, '%'))
            ORDER BY i.itemName ASC
            """)
    Page<ItemsRegister> searchAll(@Param("search") String search, Pageable pageable);
}
