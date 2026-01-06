package com.ecommerce.notificationservice.service.strategy;

import com.ecommerce.notificationservice.domain.enumtype.ChannelType;
import com.ecommerce.notificationservice.exception.VendorException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class EmailChannelStrategy implements NotificationChannelStrategy {

    private final JavaMailSender mailSender;

    // ✅ MUST be a real email, NOT SMTP username
    @Value("${spring.mail.from}")
    private String fromEmail;

    @Override
    public void send(String recipient, String subject, String messageBody) {

        log.info("[EMAIL] Connecting to SMTP server...");

        validateEmail(fromEmail, "FROM");
        validateEmail(recipient, "TO");

        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "UTF-8");

            helper.setFrom(fromEmail.trim());
            helper.setTo(recipient.trim());
            helper.setSubject(
                    (subject == null || subject.isBlank())
                            ? "Notification from E-Commerce"
                            : subject
            );
            helper.setText(messageBody, true); // HTML enabled

            mailSender.send(mimeMessage);

            log.info("[EMAIL SENT] Successfully sent to {}", recipient);

        } catch (Exception e) {
            throw new VendorException("Email delivery failed ",e.getMessage());
        }
    }

    private void validateEmail(String email, String type) {
        try {
            InternetAddress address = new InternetAddress(email);
            address.validate();
        } catch (Exception e) {
            throw new VendorException("Invalid " + type + " email address: [" + email + "]", e.getMessage());
        }
    }

    @Override
    public ChannelType getSupportedType() {
        return ChannelType.EMAIL;
    }
}
