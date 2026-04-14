package com.salespeople.communications;

import com.salespeople.common.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class CommunicationManager {

    private final MessageTemplateRepository messageTemplateRepository;
    private final EmailService emailService;

    public void sendEmail(AlertDTO alertDTO) {
        MessageTemplate template = messageTemplateRepository.findByMstType(alertDTO.getTemplateName())
                .orElseThrow(() -> new RuntimeException("Template not found: " + alertDTO.getTemplateName()));

        String emailBody = template.getMstEmail();
        String subject = template.getMstSubject();

        // Replace standard placeholders
        emailBody = replacePlaceholder(emailBody, "#{USER_FIRST_NAME}", alertDTO.getFirstName());
        subject = replacePlaceholder(subject, "#{USER_FIRST_NAME}", alertDTO.getFirstName());

        // Replace custom placeholders from map
        if (alertDTO.getPlaceholders() != null) {
            for (Map.Entry<String, String> entry : alertDTO.getPlaceholders().entrySet()) {
                emailBody = replacePlaceholder(emailBody, entry.getKey(), entry.getValue());
                subject = replacePlaceholder(subject, entry.getKey(), entry.getValue());
            }
        }

        emailService.sendHtmlEmail(alertDTO.getEmail(), subject, emailBody);
        log.info("Email sent to {} using template {}", alertDTO.getEmail(), alertDTO.getTemplateName());
    }

    private String replacePlaceholder(String original, String placeholder, String replacement) {
        if (original == null || replacement == null) {
            return original;
        }
        return original.replace(placeholder, replacement);
    }
}
