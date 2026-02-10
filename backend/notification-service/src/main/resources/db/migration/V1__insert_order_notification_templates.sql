-- =============================================
-- Notification Templates for Order Events
-- Run this script to insert order notification templates
-- =============================================

-- ORDER_PLACED Template
INSERT INTO notification_template_entity (event_type, channel_type, subject, body_template)
SELECT 'ORDER_PLACED', 'EMAIL', 
    'Order Confirmed - #{orderId}',
    'Dear #{name},\n\nThank you for your order!\n\nYour order #{orderId} has been placed successfully.\n\nOrder Details:\n- Total Amount: ₹#{totalAmount}\n\nWe will notify you once your order is shipped.\n\nThank you for shopping with us!\n\nBest regards,\nE-Commerce Team'
WHERE NOT EXISTS (SELECT 1 FROM notification_template_entity WHERE event_type = 'ORDER_PLACED' AND channel_type = 'EMAIL');

-- ORDER_SHIPPED Template
INSERT INTO notification_template_entity (event_type, channel_type, subject, body_template)
SELECT 'ORDER_SHIPPED', 'EMAIL',
    'Your Order #{orderId} Has Been Shipped!',
    'Dear #{name},\n\nGreat news! Your order #{orderId} has been shipped.\n\nOrder Details:\n- Order ID: #{orderId}\n- Total Amount: ₹#{totalAmount}\n\nYou can track your order status from your account dashboard.\n\nThank you for shopping with us!\n\nBest regards,\nE-Commerce Team'
WHERE NOT EXISTS (SELECT 1 FROM notification_template_entity WHERE event_type = 'ORDER_SHIPPED' AND channel_type = 'EMAIL');

-- ORDER_DELIVERED Template
INSERT INTO notification_template_entity (event_type, channel_type, subject, body_template)
SELECT 'ORDER_DELIVERED', 'EMAIL',
    'Order Delivered - #{orderId}',
    'Dear #{name},\n\nYour order #{orderId} has been delivered successfully!\n\nOrder Details:\n- Order ID: #{orderId}\n- Total Amount: ₹#{totalAmount}\n\nWe hope you enjoy your purchase. Please take a moment to rate your experience and leave a review.\n\nIf you have any issues with your order, please contact our support team.\n\nThank you for shopping with us!\n\nBest regards,\nE-Commerce Team'
WHERE NOT EXISTS (SELECT 1 FROM notification_template_entity WHERE event_type = 'ORDER_DELIVERED' AND channel_type = 'EMAIL');

-- ORDER_CANCELLED Template
INSERT INTO notification_template_entity (event_type, channel_type, subject, body_template)
SELECT 'ORDER_CANCELLED', 'EMAIL',
    'Order Cancelled - #{orderId}',
    'Dear #{name},\n\nYour order #{orderId} has been cancelled.\n\nOrder Details:\n- Order ID: #{orderId}\n- Total Amount: ₹#{totalAmount}\n- Cancellation Reason: #{reason}\n\nIf you did not request this cancellation or have any questions, please contact our support team.\n\nIf a payment was made, a refund will be processed within 5-7 business days.\n\nThank you for your understanding.\n\nBest regards,\nE-Commerce Team'
WHERE NOT EXISTS (SELECT 1 FROM notification_template_entity WHERE event_type = 'ORDER_CANCELLED' AND channel_type = 'EMAIL');

-- ORDER_REFUNDED Template
INSERT INTO notification_template_entity (event_type, channel_type, subject, body_template)
SELECT 'ORDER_REFUNDED', 'EMAIL',
    'Refund Processed - Order #{orderId}',
    'Dear #{name},\n\nWe have processed a refund for your order #{orderId}.\n\nRefund Details:\n- Order ID: #{orderId}\n- Refund Amount: ₹#{totalAmount}\n- Reason: #{reason}\n\nThe refund will be credited to your original payment method within 5-7 business days.\n\nIf you have any questions, please contact our support team.\n\nThank you for your patience.\n\nBest regards,\nE-Commerce Team'

WHERE NOT EXISTS (SELECT 1 FROM notification_template_entity WHERE event_type = 'ORDER_REFUNDED' AND channel_type = 'EMAIL');
