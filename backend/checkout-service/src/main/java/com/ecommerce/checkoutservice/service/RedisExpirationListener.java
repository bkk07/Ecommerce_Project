package com.ecommerce.checkoutservice.service;

import com.ecommerce.checkoutservice.entity.CheckoutSession;
import com.ecommerce.checkoutservice.repository.CheckoutSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class RedisExpirationListener implements MessageListener {

    private final CheckoutSessionRepository sessionRepository;
    private static final String SHADOW_KEY_PREFIX = "shadow:";

    @Override
    public void onMessage(Message message, byte[] pattern) {
        String expiredKey = message.toString();

        // We only care about shadow key expirations
        if (expiredKey.startsWith(SHADOW_KEY_PREFIX)) {
            String orderId = expiredKey.substring(SHADOW_KEY_PREFIX.length());
            log.info("Shadow key expired for Order ID: {}", orderId);

            // Retrieve the Data Key (which should still exist)
            Optional<CheckoutSession> sessionOpt = sessionRepository.findById(orderId);

            if (sessionOpt.isPresent()) {
                log.info("Session expired for: {}", orderId);
                // Delete the Data Key
                sessionRepository.deleteById(orderId);
            } else {
                log.warn("Data key not found for expired shadow key: {}", orderId);
            }
        }
    }
}
