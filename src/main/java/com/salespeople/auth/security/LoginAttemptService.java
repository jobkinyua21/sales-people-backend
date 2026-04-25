package com.salespeople.auth.security;

import com.salespeople.auth.user.UserTb;
import com.salespeople.auth.user.UserTbRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class LoginAttemptService {

    private final LoginAttemptRepository loginAttemptRepository;
    private final UserTbRepository userTbRepository;

    @Value("${security.max-failed-attempts:5}")
    private int maxFailedAttempts;

    @Value("${security.lock-duration-minutes:30}")
    private int lockDurationMinutes;

    @Transactional
    public void recordLoginAttempt(Long usrId, String email,
                                   boolean success, String ipAddress, String userAgent,
                                   String failureReason) {
        LoginAttempt attempt = LoginAttempt.builder()
                .usrId(usrId)
                .email(email)
                .success(success)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .failureReason(failureReason)
                .attemptedAt(LocalDateTime.now())
                .build();

        loginAttemptRepository.save(attempt);
    }

    @Transactional
    public boolean isAccountLocked(UserTb user) {
        if (user.getLockedUntil() == null) return false;

        if (user.getLockedUntil().isBefore(LocalDateTime.now())) {
            user.setFailedLoginAttempts(0);
            user.setLockedUntil(null);
            userTbRepository.save(user);
            return false;
        }

        return true;
    }

    @Transactional
    public void resetFailedAttempts(UserTb user) {
        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
        userTbRepository.save(user);
    }

    @Transactional
    public void incrementFailedAttempts(UserTb user) {
        int current = user.getFailedLoginAttempts() != null ? user.getFailedLoginAttempts() : 0;
        int attempts = current + 1;
        user.setFailedLoginAttempts(attempts);

        if (attempts >= maxFailedAttempts) {
            user.setLockedUntil(LocalDateTime.now().plusMinutes(lockDurationMinutes));
        }

        userTbRepository.save(user);
    }
}
