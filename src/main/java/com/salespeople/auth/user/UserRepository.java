package com.salespeople.auth.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByUsrEmail(String email);

    Optional<User> findByUsrPhoneNumber(String phoneNumber);

    boolean existsByUsrEmail(String email);

    boolean existsByUsrPhoneNumber(String phoneNumber);

    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);

    Optional<User> findByEmailVerificationToken(String token);

    long countByRoleId(UUID roleId);
}
