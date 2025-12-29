package com.ecommerce.notificationservice.service.strategy;

import com.ecommerce.notificationservice.domain.enumtype.ChannelType;
import com.ecommerce.notificationservice.exception.VendorException;
import jakarta.mail.MessagingException;
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

    // Optional: Inject sender email from properties or hardcode it
    @Value("${spring.mail.username}")
    private String fromEmail;

    @Override
    public void send(String recipient,String subject,String messageBody) {
        log.info("Connecting to SMTP Server...");

        try {
            // 1. Create a MIME message (Supports HTML)
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "utf-8");

            // 2. Set Email Details
            helper.setText(messageBody, true); // 'true' means enable HTML
            helper.setTo(recipient);

            String finalSubject = (subject != null && !subject.isEmpty())
                    ? subject
                    : "Notification from E-Commerce";
            helper.setSubject(finalSubject); // You can also pass subject as a param if needed
            helper.setFrom(fromEmail);

            // 3. Send
            mailSender.send(mimeMessage);

            log.info("[EMAIL SENT] Successfully sent to {}", recipient);

        } catch (MessagingException e) {
            log.error("Failed to send email to {}", recipient, e);
            // Wrap the error so the Service knows to mark it as FAILED
            throw new VendorException("Email delivery failed: " + e.getMessage());
        } catch (Exception e) {
            // Catch generic connection timeouts/auth errors
            log.error("SMTP Connection Error", e);
            throw new VendorException("SMTP Connection Error: " + e.getMessage());
        }
    }

    @Override
    public ChannelType getSupportedType() {
        return ChannelType.EMAIL;
    }
}