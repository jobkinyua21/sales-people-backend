package com.salespeople.auth.security;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OtpVerificationRepository extends JpaRepository<OtpVerification, UUID> {

    Optional<OtpVerification> findByEmailAndOtpCodeAndIsUsedFalseAndExpiresAtAfter(
            String email, String otpCode, LocalDateTime now);

    Optional<OtpVerification> findByUsrIdAndOtpCodeAndIsUsedFalseAndExpiresAtAfter(
            UUID usrId, String otpCode, LocalDateTime now);

    void deleteByExpiresAtBefore(LocalDateTime now);

    void deleteByEmailAndIsUsedFalse(String email);

    void deleteByUsrIdAndIsUsedFalse(UUID usrId);

    long countByUsrIdAndCreatedAtAfter(UUID usrId, LocalDateTime after);

    Optional<OtpVerification> findByUsrIdAndIsUsedFalseAndExpiresAtAfter(UUID usrId, LocalDateTime now);

    Optional<OtpVerification> findByEmailAndIsUsedFalseAndExpiresAtAfter(String email, LocalDateTime now);

    Optional<OtpVerification> findByResetTokenAndUsrId(String resetToken, UUID usrId);
}
