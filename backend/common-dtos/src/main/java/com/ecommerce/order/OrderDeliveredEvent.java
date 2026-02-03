package com.ecommerce.order;

import java.util.List;

/**
 * Event published when an order is delivered.
 * Rating Service consumes this to enable rating eligibility.
 */
public class OrderDeliveredEvent {
    private String orderId;
    private String userId;
    private List<DeliveredItem> items;

    public OrderDeliveredEvent() {}

    public OrderDeliveredEvent(String orderId, String userId, List<DeliveredItem> items) {
        this.orderId = orderId;
        this.userId = userId;
        this.items = items;
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public List<DeliveredItem> getItems() {
        return items;
    }

    public void setItems(List<DeliveredItem> items) {
        this.items = items;
    }

    public static class DeliveredItem {
        private String sku;
        private String productName;
        private String imageUrl;

        public DeliveredItem() {}

        public DeliveredItem(String sku, String productName, String imageUrl) {
            this.sku = sku;
            this.productName = productName;
            this.imageUrl = imageUrl;
        }

        public String getSku() {
            return sku;
        }

        public void setSku(String sku) {
            this.sku = sku;
        }

        public String getProductName() {
            return productName;
        }

        public void setProductName(String productName) {
            this.productName = productName;
        }

        public String getImageUrl() {
            return imageUrl;
        }

        public void setImageUrl(String imageUrl) {
            this.imageUrl = imageUrl;
        }
    }

    // Builder pattern
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String orderId;
        private String userId;
        private List<DeliveredItem> items;

        public Builder orderId(String orderId) {
            this.orderId = orderId;
            return this;
        }

        public Builder userId(String userId) {
            this.userId = userId;
            return this;
        }

        public Builder items(List<DeliveredItem> items) {
            this.items = items;
            return this;
        }

        public OrderDeliveredEvent build() {
            return new OrderDeliveredEvent(orderId, userId, items);
        }
    }
}
