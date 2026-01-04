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
    public static final String CREATE_ORDER_COMMAND_TOPIC = "create-order-command";
    public static final String PAYMENT_INITIATED_EVENT_TOPIC = "payment-initiated-event";


    public static final String ORDER_CANCEL_EVENTS_TOPIC = "order-cancelled-event";
    public static final String INVENTORY_RELEASED_EVENTS_TOPIC = "inventory-released-event";
    public static final String PAYMENT_REFUNDED_EVENTS_TOPIC = "payment-refunded-event";
    public static final String INVENTORY_LOCK_TOPIC = "inventory-lock-topic";
    public static final String INVENTORY_LOCK_FAILED_TOPIC = "inventory-lock-failed-topic";

    public static final String ORDER_PLACED_TOPIC = "order-placed-event";
    public static final String USER_EVENTS_TOPIC = "user-events";
}
