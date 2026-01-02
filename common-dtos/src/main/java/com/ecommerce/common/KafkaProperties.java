package com.ecommerce.common;

public class KafkaProperties {
    public static final String PRODUCT_EVENTS_TOPIC = "product-events";
    public static final String PRODUCT_EVENTS_GROUP = "product-event-group";

    public static final String CATEGORY_EVENTS_TOPIC = "category-events";
    public  static final String CATEGORY_EVENTS_GROUP = "category-event-group";

    public static final String INVENTORY_EVENTS_TOPIC = "inventory-events";
    public static final String INVENTORY_EVENTS_GROUP = "inventory-event-group";

    public static final String ORDER_EVENTS_TOPIC = "order-events";
    public static final String ORDER_EVENTS_GROUP = "order-event-group";

    public static final String PAYMENT_EVENTS_TOPIC = "payment-events";
    public static  final String PAYMENT_EVENTS_GROUP = "payment-event-group";


    public static final String PAYMENTS_EVENTS_SUCCESS_TOPIC = "payment-success-event";
    public static final String ORDER_CREATED_EVENTS_TOPIC = "order-created-event";
}
