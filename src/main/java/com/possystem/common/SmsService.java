package com.possystem.common;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class SmsService {

    @Value("${sms.api.url:}")
    private String smsApiUrl;

    @Value("${sms.api.key:}")
    private String smsApiKey;

    @Value("${sms.sender.id:POSSYSTEM}")
    private String senderId;

    public void sendSms(String phoneNumber, String message) {
        try {
            // TODO: Implement SMS provider integration (e.g., Twilio, Africa's Talking, etc.)
            log.info("Sending SMS to: {} with message: {}", phoneNumber, message);
            
            // Placeholder for actual SMS sending logic
            doSendSms(phoneNumber, message);
            
            log.info("SMS sent successfully to: {}", phoneNumber);
        } catch (Exception e) {
            log.error("Failed to send SMS to: {}", phoneNumber, e);
            throw new RuntimeException("Failed to send SMS", e);
        }
    }

    public void sendOtp(String phoneNumber, String otp) {
        String message = String.format("Your verification code is: %s. Valid for 10 minutes.", otp);
        sendSms(phoneNumber, message);
    }

    private void doSendSms(String phoneNumber, String message) {
        // Implement actual SMS provider API call here
        // Example providers: Twilio, Africa's Talking, Nexmo, etc.
    }
}
