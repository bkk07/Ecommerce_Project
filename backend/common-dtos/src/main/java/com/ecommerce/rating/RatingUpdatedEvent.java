package com.ecommerce.rating;

import java.time.Instant;

public class RatingUpdatedEvent {
    private String sku;
    private Double averageRating;
    private Long totalRatings;
    private Instant timestamp;

    public RatingUpdatedEvent() {
    }

    public RatingUpdatedEvent(String sku, Double averageRating, Long totalRatings) {
        this.sku = sku;
        this.averageRating = averageRating;
        this.totalRatings = totalRatings;
        this.timestamp = Instant.now();
    }

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    public Double getAverageRating() {
        return averageRating;
    }

    public void setAverageRating(Double averageRating) {
        this.averageRating = averageRating;
    }

    public Long getTotalRatings() {
        return totalRatings;
    }

    public void setTotalRatings(Long totalRatings) {
        this.totalRatings = totalRatings;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }
}
