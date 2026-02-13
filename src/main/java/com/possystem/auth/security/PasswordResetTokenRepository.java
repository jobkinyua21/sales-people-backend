package com.possystem.auth.security;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, UUID> {

    Optional<PasswordResetToken> findByToken(String token);

    Optional<PasswordResetToken> findByTokenHash(String tokenHash);

    Optional<PasswordResetToken> findByTokenAndIsUsedFalseAndExpiresAtAfter(String token, LocalDateTime now);

    void deleteByUsrIdAndIsUsedFalse(UUID usrId);
}
