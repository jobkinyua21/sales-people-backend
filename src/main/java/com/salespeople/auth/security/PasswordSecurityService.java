package com.salespeople.auth.security;

import com.salespeople.auth.user.User;
import com.salespeople.auth.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PasswordSecurityService {

    private final PasswordHistoryRepository passwordHistoryRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${security.password-history-count:5}")
    private int passwordHistoryCount;

    @Value("${security.password-reset-expiry-minutes:60}")
    private int resetExpiryMinutes;

    @Value("${security.password-expiry-days:90}")
    private int passwordExpiryDays;

    @Transactional
    public void savePasswordHistory(UUID usrId, String passwordHash) {
        PasswordHistory history = PasswordHistory.builder()
                .usrId(usrId)
                .passwordHash(passwordHash)
                .createdAt(LocalDateTime.now())
                .build();

        passwordHistoryRepository.save(history);
    }

    public boolean isPasswordReused(UUID usrId, String rawPassword) {
        List<PasswordHistory> histories = passwordHistoryRepository
                .findByUsrIdOrderByCreatedAtDesc(usrId);

        int count = 0;
        for (PasswordHistory history : histories) {
            if (count >= passwordHistoryCount) break;
            if (passwordEncoder.matches(rawPassword, history.getPasswordHash())) {
                return true;
            }
            count++;
        }
        return false;
    }

    public boolean isPasswordExpired(User user) {
        if (user.getPasswordExpiresAt() == null) return false;
        return user.getPasswordExpiresAt().isBefore(LocalDateTime.now());
    }

    @Transactional
    public PasswordResetToken generatePasswordResetToken(UUID usrId, String ipAddress, String userAgent) {
        // Invalidate any existing unused tokens for this user
        passwordResetTokenRepository.deleteByUsrIdAndIsUsedFalse(usrId);

        String token = UUID.randomUUID().toString();
        String tokenHash = passwordEncoder.encode(token);

        PasswordResetToken resetToken = PasswordResetToken.builder()
                .usrId(usrId)
                .token(token)
                .tokenHash(tokenHash)
                .isUsed(false)
                .expiresAt(LocalDateTime.now().plusMinutes(resetExpiryMinutes))
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .createdAt(LocalDateTime.now())
                .build();

        return passwordResetTokenRepository.save(resetToken);
    }

    public PasswordResetToken validateResetToken(String token) {
        return passwordResetTokenRepository
                .findByTokenAndIsUsedFalseAndExpiresAtAfter(token, LocalDateTime.now())
                .orElse(null);
    }

    @Transactional
    public void resetPassword(String token, String newPassword) {
        PasswordResetToken resetToken = validateResetToken(token);
        if (resetToken == null) {
            throw new IllegalArgumentException("Invalid or expired password reset token");
        }

        User user = userRepository.findById(resetToken.getUsrId())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // Update password
        user.setUsrPassword(passwordEncoder.encode(newPassword));
        user.setPasswordVersion(user.getPasswordVersion() + 1);
        user.setPasswordChangedAt(LocalDateTime.now());
        user.setPasswordExpiresAt(LocalDateTime.now().plusDays(passwordExpiryDays));
        user.setMustChangePassword(false);
        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
        userRepository.save(user);

        // Mark token as used
        resetToken.setIsUsed(true);
        resetToken.setUsedAt(LocalDateTime.now());
        passwordResetTokenRepository.save(resetToken);
    }
}
