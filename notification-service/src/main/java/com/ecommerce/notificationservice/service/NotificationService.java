package com.ecommerce.notificationservice.service;

import com.ecommerce.notificationservice.domain.enumtype.NotificationStatus;
import com.ecommerce.notificationservice.domain.model.NotificationLog;
import com.ecommerce.notificationservice.domain.model.NotificationTemplate;
import com.ecommerce.notificationservice.domain.port.NotificationRepositoryPort;
import com.ecommerce.notificationservice.domain.port.TemplateRepositoryPort;
import com.ecommerce.notificationservice.exception.TemplateNotFoundException;
import com.ecommerce.notificationservice.infrastructure.events.NotificationEvent;
import com.ecommerce.notificationservice.service.factory.ChannelStrategyFactory;
import com.ecommerce.notificationservice.service.strategy.NotificationChannelStrategy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    // CHANGED: Accepts NotificationEvent now
    public void processNotification(NotificationEvent event) {
        log.info("Processing notification for: {} (EventID: {})", event.getRecipient(), event.getEventId());

        // 1. Fetch Template
        NotificationTemplate template = templateRepository.findByEventTypeAndChannel(
                event.getEventType(),
                event.getChannel() // CHANGED: getChannelType() -> getChannel()
        ).orElseThrow(() -> new TemplateNotFoundException("No template found for " + event.getEventType()));

        // 2. Render Body AND Subject
        // CHANGED: getParams() -> getPayload()
        String body = renderContent(template.getBodyTemplate(), event.getPayload());
        String subject = renderContent(template.getSubject(), event.getPayload());

        // 3. Create Log
        NotificationLog logEntry = NotificationLog.builder()
                .eventId(event.getEventId())       // <--- NEW: Store Event ID for Idempotency
                .recipient(event.getRecipient())
                .channelType(event.getChannel())   // CHANGED
                .content(body)
                .status(NotificationStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .retryCount(0)
                .build();

        logEntry = notificationRepository.save(logEntry);

        // 4. Send via Strategy
        try {
            NotificationChannelStrategy strategy = strategyFactory.getStrategy(event.getChannel());

            // Pass Subject AND Body
            strategy.send(event.getRecipient(), subject, body);

            logEntry.setStatus(NotificationStatus.SENT);
        } catch (Exception e) {
            log.error("Failed to send notification", e);

            // Update DB status to FAILED
            logEntry.setStatus(NotificationStatus.FAILED);
            logEntry.setErrorMessage(e.getMessage());

            // <--- CRITICAL FIX: Rethrow exception
            // This ensures the Consumer knows it failed, triggering Retry -> DLQ.
            throw new RuntimeException("Vendor sending failed", e);
        } finally {
            notificationRepository.save(logEntry);
        }
    }

    // Safer Render Method (Handles nulls)
    public String renderContent(String template, Map<String, String> params) {
        if (template == null) return "";
        String result = template;
        if (params != null) {
            for (Map.Entry<String, String> entry : params.entrySet()) {
                String key = "\\{" + entry.getKey() + "\\}"; // Regex escape
                String value = entry.getValue() != null ? entry.getValue() : "";
                result = result.replaceAll(key, value);
            }
        }
        return result;
    }
}