package com.possystem.auth.security;

import com.possystem.auth.user.User;
import com.possystem.auth.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EmailVerificationService {

    private final UserRepository userRepository;

    @Transactional
    public String generateVerificationToken(User user) {
        String token = UUID.randomUUID().toString();
        user.setEmailVerificationToken(token);
        userRepository.save(user);
        return token;
    }

    @Transactional
    public User verifyEmail(String token) {
        User user = userRepository.findByEmailVerificationToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Invalid verification token"));

        if (Boolean.TRUE.equals(user.getEmailVerified())) {
            throw new IllegalArgumentException("Email is already verified");
        }

        user.setEmailVerified(true);
        user.setEmailVerifiedAt(LocalDateTime.now());
        user.setEmailVerificationToken(null);

        return userRepository.save(user);
    }

    @Transactional
    public String resendVerificationToken(String email) {
        User user = userRepository.findByUsrEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (Boolean.TRUE.equals(user.getEmailVerified())) {
            throw new IllegalArgumentException("Email is already verified");
        }

        return generateVerificationToken(user);
    }
}
