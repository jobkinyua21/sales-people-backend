package com.salespeople.auth.security;

import com.salespeople.auth.user.User;
import com.salespeople.auth.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LoginAttemptService {

    private final LoginAttemptRepository loginAttemptRepository;
    private final UserRepository userRepository;

    @Value("${security.max-failed-attempts:5}")
    private int maxFailedAttempts;

    @Value("${security.lock-duration-minutes:30}")
    private int lockDurationMinutes;

    @Transactional
    public void recordLoginAttempt(UUID usrId, String username, String email,
                                   boolean success, String ipAddress, String userAgent,
                                   String failureReason) {
        LoginAttempt attempt = LoginAttempt.builder()
                .usrId(usrId)
                .username(username)
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
    public boolean isAccountLocked(User user) {
        if (user.getLockedUntil() == null) {
            return false;
        }

        // Lock has expired — auto-unlock
        if (user.getLockedUntil().isBefore(LocalDateTime.now())) {
            user.setFailedLoginAttempts(0);
            user.setLockedUntil(null);
            userRepository.save(user);
            return false;
        }

        return true;
    }

    @Transactional
    public void resetFailedAttempts(User user) {
        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
        userRepository.save(user);
    }

    @Transactional
    public void incrementFailedAttempts(User user) {
        int current = user.getFailedLoginAttempts() != null ? user.getFailedLoginAttempts() : 0;
        int attempts = current + 1;
        user.setFailedLoginAttempts(attempts);

        if (attempts >= maxFailedAttempts) {
            user.setLockedUntil(LocalDateTime.now().plusMinutes(lockDurationMinutes));
        }

        userRepository.save(user);
    }
}
