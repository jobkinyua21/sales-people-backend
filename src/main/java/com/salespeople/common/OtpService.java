package com.salespeople.common;

import com.salespeople.auth.security.OtpVerification;
import com.salespeople.auth.security.OtpVerificationRepository;
import com.salespeople.communications.AlertDTO;
import com.salespeople.communications.CommunicationManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class OtpService {

    private static final SecureRandom random = new SecureRandom();

    private final OtpVerificationRepository otpVerificationRepository;
    private final CommunicationManager communicationManager;

    @Value("${security.otp-expiry-minutes:5}")
    private int otpExpiryMinutes;

    @Value("${security.otp-max-requests:3}")
    private int otpMaxRequests;

    @Value("${security.otp-rate-limit-minutes:5}")
    private int otpRateLimitMinutes;

    @Value("${security.otp-max-wrong-attempts:5}")
    private int otpMaxWrongAttempts;

    public String generateOtp() {
        int otp = random.nextInt(9000) + 1000;
        return String.valueOf(otp);
    }

    @Transactional
    public void createAndSendOtp(UUID usrId, String email, String firstName, String userType, String ipAddress) {
        // Rate limit: max N OTP requests within the configured window
        LocalDateTime rateLimitWindow = LocalDateTime.now().minusMinutes(otpRateLimitMinutes);
        long recentOtpCount = otpVerificationRepository.countByUsrIdAndCreatedAtAfter(usrId, rateLimitWindow);
        if (recentOtpCount >= otpMaxRequests) {
            throw new IllegalStateException(ErrorCode.OTP_RATE_LIMIT.getDefaultMessage());
        }

        // Invalidate any existing unused OTPs for this email
        otpVerificationRepository.deleteByEmailAndIsUsedFalse(email);

        String otpCode = generateOtp();

        OtpVerification otp = OtpVerification.builder()
                .usrId(usrId)
                .userType(userType)
                .otpCode(otpCode)
                .email(email)
                .expiresAt(LocalDateTime.now().plusMinutes(otpExpiryMinutes))
                .ipAddress(ipAddress)
                .build();

        otpVerificationRepository.save(otp);

        log.info("OTP code for {} (user {}): {}", email, usrId, otpCode);

        // Send OTP via email template
        AlertDTO alert = AlertDTO.builder()
                .email(email)
                .firstName(firstName)
                .templateName("LOGIN_OTP")
                .usrId(usrId)
                .placeholders(Map.of(
                        "#{OTP_CODE}", otpCode,
                        "#{EXPIRY_MINUTES}", String.valueOf(otpExpiryMinutes)
                ))
                .build();

        try {
            communicationManager.sendEmail(alert);
        } catch (Exception e) {
            log.error("Failed to send OTP email to {}: {}", email, e.getMessage());
        }

        log.info("OTP created and sent to {} for user {}", email, usrId);
    }

    @Transactional
    public OtpVerification verifyOtpByUserId(UUID usrId, String otpCode) {
        var matched = otpVerificationRepository
                .findByUsrIdAndOtpCodeAndIsUsedFalseAndExpiresAtAfter(usrId, otpCode, LocalDateTime.now());

        if (matched.isPresent()) {
            OtpVerification otp = matched.get();
            otp.setIsUsed(true);
            otp.setUsedAt(LocalDateTime.now());
            otpVerificationRepository.save(otp);
            return otp;
        }

        // Wrong OTP — find the active OTP and increment attempt count
        var activeOtp = otpVerificationRepository
                .findByUsrIdAndIsUsedFalseAndExpiresAtAfter(usrId, LocalDateTime.now());

        if (activeOtp.isPresent()) {
            OtpVerification otp = activeOtp.get();
            otp.setAttemptCount(otp.getAttemptCount() + 1);

            if (otp.getAttemptCount() >= otpMaxWrongAttempts) {
                otp.setIsUsed(true);
                otpVerificationRepository.save(otp);
                throw new IllegalStateException(ErrorCode.OTP_MAX_ATTEMPTS.getDefaultMessage());
            }

            otpVerificationRepository.save(otp);
        }

        throw new IllegalArgumentException(ErrorCode.INVALID_OTP.getDefaultMessage());
    }
}
