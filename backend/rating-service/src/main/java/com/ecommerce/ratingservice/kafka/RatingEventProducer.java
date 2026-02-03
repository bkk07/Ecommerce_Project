package com.ecommerce.ratingservice.kafka;

import com.ecommerce.rating.RatingUpdatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import static com.ecommerce.common.KafkaProperties.RATING_EVENTS_TOPIC;

@Service
@RequiredArgsConstructor
@Slf4j
public class RatingEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publishRatingUpdated(String sku, Double averageRating, Long totalRatings) {
        log.info("========================================");
        log.info("PUBLISHING RATING UPDATED EVENT");
        log.info("SKU: {}", sku);
        log.info("Average Rating: {}", averageRating);
        log.info("Total Ratings: {}", totalRatings);
        log.info("Topic: {}", RATING_EVENTS_TOPIC);
        log.info("========================================");

        RatingUpdatedEvent event = new RatingUpdatedEvent(sku, averageRating, totalRatings);
        kafkaTemplate.send(RATING_EVENTS_TOPIC, sku, event);
    }
}
