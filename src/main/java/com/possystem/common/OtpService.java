package com.possystem.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;

@Slf4j
@Service
public class OtpService {

    private static final SecureRandom random = new SecureRandom();
    private static final int OTP_LENGTH = 4;

    public String generateOtp() {
        int otp = random.nextInt(9000) + 1000; // Generates 1000-9999
        log.debug("OTP generated");
        return String.valueOf(otp);
    }
}
