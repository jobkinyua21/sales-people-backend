package com.possystem.auth.security;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserSessionService {

    private final UserSessionRepository userSessionRepository;

    @Value("${jwt.expiration:86400000}")
    private long jwtExpiration;

    @Transactional
    public UserSession createSession(UUID usrId, String accessToken, String refreshToken,
                                     UUID tenantId, UUID shopId, String context,
                                     String ipAddress, String userAgent) {
        UserSession session = UserSession.builder()
                .usrId(usrId)
                .sessionToken(accessToken)
                .refreshToken(refreshToken)
                .currentTenantId(tenantId)
                .currentShopId(shopId)
                .currentContext(context != null ? context : "TENANT")
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .expiresAt(LocalDateTime.now().plusSeconds(jwtExpiration / 1000))
                .createdAt(LocalDateTime.now())
                .lastActivityAt(LocalDateTime.now())
                .build();

        return userSessionRepository.save(session);
    }

    @Transactional
    public void invalidateSession(String sessionToken) {
        userSessionRepository.findBySessionToken(sessionToken)
                .ifPresent(userSessionRepository::delete);
    }

    public Optional<UserSession> findByRefreshToken(String refreshToken) {
        return userSessionRepository.findByRefreshToken(refreshToken);
    }

    @Transactional
    public void updateLastActivity(UUID sessionId) {
        userSessionRepository.findById(sessionId).ifPresent(session -> {
            session.setLastActivityAt(LocalDateTime.now());
            userSessionRepository.save(session);
        });
    }

    @Transactional
    public void invalidateAllUserSessions(UUID usrId) {
        userSessionRepository.deleteAllByUsrId(usrId);
    }

    @Transactional
    public void cleanupExpiredSessions() {
        userSessionRepository.deleteExpiredSessions(LocalDateTime.now());
    }
}
