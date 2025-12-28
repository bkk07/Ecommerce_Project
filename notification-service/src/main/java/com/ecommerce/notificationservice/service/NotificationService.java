package com.ecommerce.notificationservice.service;

import com.ecommerce.notificationservice.domain.enumtype.NotificationStatus;
import com.ecommerce.notificationservice.domain.model.NotificationLog;
import com.ecommerce.notificationservice.domain.model.NotificationTemplate;
import com.ecommerce.notificationservice.domain.port.NotificationRepositoryPort;
import com.ecommerce.notificationservice.domain.port.TemplateRepositoryPort;
import com.ecommerce.notificationservice.dto.NotificationRequest;
import com.ecommerce.notificationservice.exception.TemplateNotFoundException;
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

    public void processNotification(NotificationRequest request) {
        log.info("Processing notification for: {}", request.getRecipient());

        // 1. Fetch Template
        NotificationTemplate template = templateRepository.findByEventTypeAndChannel(request.getEventType(), request.getChannelType())
                .orElseThrow(() -> new TemplateNotFoundException("No template found for " + request.getEventType()));
        // 2. Render Message (Simple variable replacement)
        String content = renderContent(template.getBodyTemplate(), request.getParams());

        // 3. Create Initial Log (PENDING)
        NotificationLog logEntry = NotificationLog.builder()
                .recipient(request.getRecipient())
                .channelType(request.getChannelType())
                .content(content)
                .status(NotificationStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .retryCount(0)
                .build();

        logEntry = notificationRepository.save(logEntry);

        // 4. Send via Strategy
        try {
            NotificationChannelStrategy strategy = strategyFactory.getStrategy(request.getChannelType());
            strategy.send(request.getRecipient(), content);

            // 5. Update Status -> SENT
            logEntry.setStatus(NotificationStatus.SENT);
        } catch (Exception e) {
            log.error("Failed to send notification", e);
            // 5. Update Status -> FAILED
            logEntry.setStatus(NotificationStatus.FAILED);
            logEntry.setErrorMessage(e.getMessage());
        } finally {
            notificationRepository.save(logEntry);
        }
    }

    public String renderContent(String template, Map<String, String> params) {
        String result = template;
        for (Map.Entry<String, String> entry : params.entrySet()) {
            result = result.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        return result;
    }
}