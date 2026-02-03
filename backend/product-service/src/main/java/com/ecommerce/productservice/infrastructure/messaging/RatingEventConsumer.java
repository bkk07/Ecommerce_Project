package com.ecommerce.productservice.infrastructure.messaging;

import com.ecommerce.productservice.domain.repository.ProductVariantRepository;
import com.ecommerce.rating.RatingUpdatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.ecommerce.common.KafkaProperties.RATING_EVENTS_TOPIC;

@Service
@RequiredArgsConstructor
@Slf4j
public class RatingEventConsumer {

    private final ProductVariantRepository productVariantRepository;

    @KafkaListener(topics = RATING_EVENTS_TOPIC, groupId = "product-rating-group")
    @Transactional
    public void handleRatingUpdate(RatingUpdatedEvent event) {
        log.info("========================================");
        log.info("RECEIVED RATING UPDATE EVENT in Product Service");
        log.info("SKU: {}", event.getSku());
        log.info("Average Rating: {}", event.getAverageRating());
        log.info("Total Ratings: {}", event.getTotalRatings());
        log.info("========================================");

        try {
            int updated = productVariantRepository.updateRatingBySku(
                    event.getSku(),
                    event.getAverageRating(),
                    event.getTotalRatings()
            );

            if (updated > 0) {
                log.info("Successfully updated rating for SKU: {} - Avg: {}, Count: {}",
                        event.getSku(), event.getAverageRating(), event.getTotalRatings());
            } else {
                log.warn("No product variant found for SKU: {}", event.getSku());
            }
        } catch (Exception e) {
            log.error("Failed to update rating for SKU: {}", event.getSku(), e);
        }
    }
}
