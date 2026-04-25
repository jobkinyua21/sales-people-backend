package com.salespeople.auth.security;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OtpVerificationRepository extends JpaRepository<OtpVerification, UUID> {

    Optional<OtpVerification> findByUsrIdAndOtpCodeAndIsUsedFalseAndExpiresAtAfter(
            Long usrId, String otpCode, LocalDateTime now);

    void deleteByEmailAndIsUsedFalse(String email);

    void deleteByUsrIdAndIsUsedFalse(Long usrId);

    long countByUsrIdAndCreatedAtAfter(Long usrId, LocalDateTime after);

    Optional<OtpVerification> findByUsrIdAndIsUsedFalseAndExpiresAtAfter(Long usrId, LocalDateTime now);

    Optional<OtpVerification> findByResetTokenAndUsrId(String resetToken, Long usrId);
}
