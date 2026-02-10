package com.ecommerce.notificationservice.config;

import com.ecommerce.notificationservice.domain.enumtype.ChannelType;
import com.ecommerce.notificationservice.infrastructure.entity.NotificationTemplateEntity;
import com.ecommerce.notificationservice.infrastructure.repository.JpaTemplateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Initializes notification templates on application startup.
 * Templates are only created if they don't already exist (idempotent).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationTemplateInitializer implements ApplicationRunner {

    private final JpaTemplateRepository templateRepository;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        log.info("Initializing notification templates...");
        
        initializeOrderPlacedTemplate();
        initializeOrderShippedTemplate();
        initializeOrderDeliveredTemplate();
        initializeOrderCancelledTemplate();
        initializeOrderRefundedTemplate();
        
        log.info("Notification templates initialization completed.");
    }

    private void initializeOrderPlacedTemplate() {
        String eventType = "ORDER_PLACED";
        if (templateRepository.findByEventTypeAndChannelType(eventType, ChannelType.EMAIL).isEmpty()) {
            NotificationTemplateEntity template = NotificationTemplateEntity.builder()
                    .eventType(eventType)
                    .channelType(ChannelType.EMAIL)
                    .subject("Order Confirmed - #{orderId}")
                    .bodyTemplate("""
                            Dear #{name},
                            
                            Thank you for your order!
                            
                            Your order #{orderId} has been placed successfully.
                            
                            Order Details:
                            - Total Amount: ₹#{totalAmount}
                            
                            We will notify you once your order is shipped.
                            
                            Thank you for shopping with us!
                            
                            Best regards,
                            E-Commerce Team""")
                    .build();
            templateRepository.save(template);
            log.info("Created notification template for: {}", eventType);
        }
    }

    private void initializeOrderShippedTemplate() {
        String eventType = "ORDER_SHIPPED";
        if (templateRepository.findByEventTypeAndChannelType(eventType, ChannelType.EMAIL).isEmpty()) {
            NotificationTemplateEntity template = NotificationTemplateEntity.builder()
                    .eventType(eventType)
                    .channelType(ChannelType.EMAIL)
                    .subject("Your Order #{orderId} Has Been Shipped!")
                    .bodyTemplate("""
                            Dear #{name},
                            
                            Great news! Your order #{orderId} has been shipped.
                            
                            Order Details:
                            - Order ID: #{orderId}
                            - Total Amount: ₹#{totalAmount}
                            
                            You can track your order status from your account dashboard.
                            
                            Thank you for shopping with us!
                            
                            Best regards,
                            E-Commerce Team""")
                    .build();
            templateRepository.save(template);
            log.info("Created notification template for: {}", eventType);
        }
    }

    private void initializeOrderDeliveredTemplate() {
        String eventType = "ORDER_DELIVERED";
        if (templateRepository.findByEventTypeAndChannelType(eventType, ChannelType.EMAIL).isEmpty()) {
            NotificationTemplateEntity template = NotificationTemplateEntity.builder()
                    .eventType(eventType)
                    .channelType(ChannelType.EMAIL)
                    .subject("Order Delivered - #{orderId}")
                    .bodyTemplate("""
                            Dear #{name},
                            
                            Your order #{orderId} has been delivered successfully!
                            
                            Order Details:
                            - Order ID: #{orderId}
                            - Total Amount: ₹#{totalAmount}
                            
                            We hope you enjoy your purchase. Please take a moment to rate your experience and leave a review.
                            
                            If you have any issues with your order, please contact our support team.
                            
                            Thank you for shopping with us!
                            
                            Best regards,
                            E-Commerce Team""")
                    .build();
            templateRepository.save(template);
            log.info("Created notification template for: {}", eventType);
        }
    }

    private void initializeOrderCancelledTemplate() {
        String eventType = "ORDER_CANCELLED";
        if (templateRepository.findByEventTypeAndChannelType(eventType, ChannelType.EMAIL).isEmpty()) {
            NotificationTemplateEntity template = NotificationTemplateEntity.builder()
                    .eventType(eventType)
                    .channelType(ChannelType.EMAIL)
                    .subject("Order Cancelled - #{orderId}")
                    .bodyTemplate("""
                            Dear #{name},
                            
                            Your order #{orderId} has been cancelled.
                            
                            Order Details:
                            - Order ID: #{orderId}
                            - Total Amount: ₹#{totalAmount}
                            - Cancellation Reason: #{reason}
                            
                            If you did not request this cancellation or have any questions, please contact our support team.
                            
                            If a payment was made, a refund will be processed within 5-7 business days.
                            
                            Thank you for your understanding.
                            
                            Best regards,
                            E-Commerce Team""")
                    .build();
            templateRepository.save(template);
            log.info("Created notification template for: {}", eventType);
        }
    }

    private void initializeOrderRefundedTemplate() {
        String eventType = "ORDER_REFUNDED";
        if (templateRepository.findByEventTypeAndChannelType(eventType, ChannelType.EMAIL).isEmpty()) {
            NotificationTemplateEntity template = NotificationTemplateEntity.builder()
                    .eventType(eventType)
                    .channelType(ChannelType.EMAIL)
                    .subject("Refund Processed - Order #{orderId}")
                    .bodyTemplate("""
                            Dear #{name},
                            
                            We have processed a refund for your order #{orderId}.
                            
                            Refund Details:
                            - Order ID: #{orderId}
                            - Refund Amount: ₹#{totalAmount}
                            - Reason: #{reason}
                            
                            The refund will be credited to your original payment method within 5-7 business days.
                            
                            If you have any questions, please contact our support team.
                            
                            Thank you for your patience.
                            
                            Best regards,
                            E-Commerce Team""")
                    .build();
            templateRepository.save(template);
            log.info("Created notification template for: {}", eventType);
        }
    }
}
