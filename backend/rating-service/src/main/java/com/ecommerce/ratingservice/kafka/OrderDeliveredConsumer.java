package com.ecommerce.ratingservice.kafka;

import com.ecommerce.order.OrderDeliveredEvent;
import com.ecommerce.ratingservice.entity.RatingEligibility;
import com.ecommerce.ratingservice.repository.RatingEligibilityRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.ecommerce.common.KafkaProperties.ORDER_DELIVERED_TOPIC;

/**
 * Consumes ORDER_DELIVERED events and creates rating eligibility records.
 * This ensures only users who have purchased and received products can rate them.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OrderDeliveredConsumer {

    private final RatingEligibilityRepository eligibilityRepository;

    @KafkaListener(
            topics = ORDER_DELIVERED_TOPIC,
            groupId = "rating-eligibility-group",
            containerFactory = "orderDeliveredKafkaListenerContainerFactory"
    )
    @Transactional
    public void handleOrderDelivered(OrderDeliveredEvent event) {
        log.info("========================================");
        log.info("RECEIVED ORDER_DELIVERED EVENT");
        log.info("Order ID: {}", event.getOrderId());
        log.info("User ID: {}", event.getUserId());
        log.info("Items Count: {}", event.getItems() != null ? event.getItems().size() : 0);
        log.info("========================================");

        if (event.getItems() == null || event.getItems().isEmpty()) {
            log.warn("No items in ORDER_DELIVERED event for order: {}", event.getOrderId());
            return;
        }

        for (OrderDeliveredEvent.DeliveredItem item : event.getItems()) {
            try {
                // Check if eligibility already exists (idempotency)
                if (eligibilityRepository.findByOrderIdAndSku(event.getOrderId(), item.getSku()).isPresent()) {
                    log.info("Eligibility already exists for order: {}, sku: {}", 
                            event.getOrderId(), item.getSku());
                    continue;
                }

                // Create eligibility record
                RatingEligibility eligibility = RatingEligibility.builder()
                        .orderId(event.getOrderId())
                        .userId(event.getUserId())
                        .sku(item.getSku())
                        .productName(item.getProductName())
                        .imageUrl(item.getImageUrl())
                        .canRate(true)
                        .hasRated(false)
                        .build();

                eligibilityRepository.save(eligibility);
                log.info("Created rating eligibility for user: {}, order: {}, sku: {}",
                        event.getUserId(), event.getOrderId(), item.getSku());

            } catch (Exception e) {
                log.error("Failed to create eligibility for order: {}, sku: {}", 
                        event.getOrderId(), item.getSku(), e);
            }
        }

        log.info("Finished processing ORDER_DELIVERED event for order: {}", event.getOrderId());
    }
}
