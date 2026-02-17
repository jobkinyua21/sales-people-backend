package com.possystem.auth.user;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByUsrEmail(String email);

    Optional<User> findByUsrPhoneNumber(String phoneNumber);

    Optional<User> findByUsrEmailOrUsrPhoneNumber(String email, String phoneNumber);

    boolean existsByUsrEmail(String email);

    boolean existsByUsrPhoneNumber(String phoneNumber);

    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);

    Optional<User> findByEmailVerificationToken(String token);

    @Query("SELECT u FROM User u JOIN UserShopAssignment usa ON u.usrId = usa.userId " +
            "WHERE usa.shopId = :shopId AND usa.isActive = true AND " +
            "(:search IS NULL OR :search = '' OR " +
            "LOWER(u.usrFirstName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(u.usrLastName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(u.usrEmail) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(u.usrPhoneNumber) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(u.username) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(CAST(u.userType AS string)) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(CAST(u.usrStatus AS string)) LIKE LOWER(CONCAT('%', :search, '%'))) " +
            "ORDER BY u.createdAt DESC")
    Page<User> searchByShopAssignment(@Param("shopId") UUID shopId, @Param("search") String search, Pageable pageable);

    @Query("SELECT u FROM User u JOIN UserShopAssignment usa ON u.usrId = usa.userId " +
            "WHERE usa.shopId = :shopId AND usa.isActive = true AND " +
            "(:search IS NULL OR :search = '' OR " +
            "LOWER(u.usrFirstName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(u.usrLastName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(u.usrEmail) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(u.usrPhoneNumber) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(u.username) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(CAST(u.userType AS string)) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(CAST(u.usrStatus AS string)) LIKE LOWER(CONCAT('%', :search, '%'))) " +
            "ORDER BY u.createdAt DESC")
    List<User> searchByShopAssignment(@Param("shopId") UUID shopId, @Param("search") String search);

    long countByRoleId(UUID roleId);
}
