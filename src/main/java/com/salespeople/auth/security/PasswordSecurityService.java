package com.salespeople.auth.security;

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
    private final PasswordEncoder passwordEncoder;

    @Value("${security.password-history-count:5}")
    private int passwordHistoryCount;

    @Value("${security.password-reset-expiry-minutes:60}")
    private int resetExpiryMinutes;

    @Transactional
    public void savePasswordHistory(Long usrId, String passwordHash) {
        PasswordHistory history = PasswordHistory.builder()
                .usrId(usrId)
                .passwordHash(passwordHash)
                .createdAt(LocalDateTime.now())
                .build();

        passwordHistoryRepository.save(history);
    }

    public boolean isPasswordReused(Long usrId, String rawPassword) {
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

    @Transactional
    public PasswordResetToken generatePasswordResetToken(Long usrId, String ipAddress, String userAgent) {
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
}
