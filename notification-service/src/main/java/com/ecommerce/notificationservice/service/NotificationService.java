package com.ecommerce.notificationservice.service;

import com.ecommerce.notification.NotificationEvent;
import com.ecommerce.notificationservice.domain.enumtype.NotificationStatus;
import com.ecommerce.notificationservice.domain.model.NotificationLog;
import com.ecommerce.notificationservice.domain.model.NotificationTemplate;
import com.ecommerce.notificationservice.domain.port.NotificationRepositoryPort;
import com.ecommerce.notificationservice.domain.port.TemplateRepositoryPort;
import com.ecommerce.notificationservice.exception.TemplateNotFoundException;
import com.ecommerce.notificationservice.infrastructure.mapper.NotificationChannelMapper;
import com.ecommerce.notificationservice.service.factory.ChannelStrategyFactory;
import com.ecommerce.notificationservice.service.strategy.NotificationChannelStrategy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final TemplateRepositoryPort templateRepository;
    private final NotificationRepositoryPort notificationRepository;
    private final ChannelStrategyFactory strategyFactory;

    // DEV namespace (Testmail)
    @Value("${testmail.namespace:}")
    private String testmailNamespace;

    public void processNotification(NotificationEvent event) {

        log.info("Processing notification for: {} (EventID: {})",
                event.getRecipient(), event.getEventId());

        // 1️⃣ Fetch template
        NotificationTemplate template =
                templateRepository.findByEventTypeAndChannel(
                        event.getEventType(),
                        NotificationChannelMapper.toDomain(event.getChannel())
                ).orElseThrow(() ->
                        new TemplateNotFoundException(
                                "No template found for " + event.getEventType()
                        )
                );

        // 2️⃣ Render subject & body
        String body = renderContent(template.getBodyTemplate(), event.getPayload());
        String subject = renderContent(template.getSubject(), event.getPayload());

        // 3️⃣ Resolve recipient email (🔥 DEV FIX)
        // Pass eventType to generate dynamic tag if needed
        String resolvedRecipient = resolveRecipientEmail(event.getRecipient(), event.getEventType());

        log.info("Resolved recipient email: {}", resolvedRecipient);

        // 4️⃣ Create notification log
        NotificationLog logEntry = NotificationLog.builder()
                .eventId(event.getEventId())
                .recipient(resolvedRecipient)
                .channelType(NotificationChannelMapper.toDomain(event.getChannel()))
                .content(body)
                .status(NotificationStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .retryCount(0)
                .build();

        logEntry = notificationRepository.save(logEntry);

        // 5️⃣ Send via strategy
        try {
            NotificationChannelStrategy strategy =
                    strategyFactory.getStrategy(
                            NotificationChannelMapper.toDomain(event.getChannel())
                    );

            strategy.send(resolvedRecipient, subject, body);

            logEntry.setStatus(NotificationStatus.SENT);

        } catch (Exception e) {
            log.error("Failed to send notification", e);

            logEntry.setStatus(NotificationStatus.FAILED);

            String errorMsg = e.getMessage();
            if (errorMsg != null && errorMsg.length() > 255) {
                errorMsg = errorMsg.substring(0, 255);
            }
            logEntry.setErrorMessage(errorMsg);

            // 🚨 Rethrow → Kafka Retry / DLQ
            throw new RuntimeException("Vendor sending failed", e);

        } finally {
            notificationRepository.save(logEntry);
        }
    }

    /**
     * 🔥 DEV-ONLY email resolution
     * PROD → expects real email
     * DEV  → maps username/tag to Testmail inbox
     *
     * Logic:
     * 1. If recipient is already an email -> use it.
     * 2. If namespace is configured:
     *    - If recipient is a simple string (e.g. "login"), treat it as a tag.
     *    - Construct: {namespace}.{tag}@inbox.testmail.app
     */
    private String resolveRecipientEmail(String recipient, String eventType) {

        // Already an email → use as-is
        if (recipient != null && recipient.contains("@")) {
            return recipient;
        }

        // DEV ONLY: map to Testmail inbox
        if (testmailNamespace != null && !testmailNamespace.isBlank()) {
            // Use the recipient string as the tag (e.g., "login", "signup")
            // Or fallback to eventType if recipient is generic
            String tag = (recipient != null && !recipient.isBlank()) 
                         ? recipient.toLowerCase().replaceAll("[^a-z0-9]", "") 
                         : eventType.toLowerCase().replaceAll("[^a-z0-9]", "");
            
            return testmailNamespace + "." + tag + "@inbox.testmail.app";
        }

        // Fallback (should never happen in DEV)
        throw new IllegalArgumentException("Invalid recipient: " + recipient);
    }

    // Safe template renderer
    public String renderContent(String template, Map<String, String> params) {
        if (template == null) return "";
        String result = template;
        if (params != null) {
            for (Map.Entry<String, String> entry : params.entrySet()) {
                String key = "\\{" + entry.getKey() + "\\}";
                String value = entry.getValue() != null ? entry.getValue() : "";
                result = result.replaceAll(key, value);
            }
        }
        return result;
    }
}
