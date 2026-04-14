package com.salespeople.auth.security;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface TwoFactorAuthRepository extends JpaRepository<TwoFactorAuth, UUID> {

    Optional<TwoFactorAuth> findByUsrId(UUID usrId);

    boolean existsByUsrIdAndIsEnabledTrue(UUID usrId);
}
