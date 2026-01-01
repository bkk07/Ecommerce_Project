package com.ecommerce.paymentservice.entity;
import com.ecommerce.paymentservice.enums.PaymentMethodType;
import com.ecommerce.paymentservice.enums.PaymentStatus;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payments", indexes = {
        @Index(name = "idx_razorpay_order_id", columnList = "razorpayOrderId"),
        @Index(name = "idx_checkout_id", columnList = "checkoutId")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long userId;

    @Column(nullable = false, unique = true)
    private String checkoutId;

    @Column(nullable = false, unique = true)
    private String razorpayOrderId;

    private String razorpayPaymentId;
    private String razorpaySignature;

    @Column(nullable = false)
    private BigDecimal amount; // Stored in base currency (e.g., Rupees)

    @Column(nullable = false)
    private String currency;

    @Enumerated(EnumType.STRING)
    private PaymentStatus status;

    // --- Payment Method Details (Captured from Webhook) ---
    @Enumerated(EnumType.STRING)
    private PaymentMethodType methodType;

    private String bank;        // For Netbanking
    private String wallet;      // For Wallets
    private String vpa;         // For UPI
    private String cardNetwork; // For Cards (Visa/Mastercard)
    private String cardLast4;   // For Cards
    private String email;       // Payer Email
    private String contact;     // Payer Phone

    @Version
    private Integer version;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() { createdAt = LocalDateTime.now(); }

    @PreUpdate
    protected void onUpdate() { updatedAt = LocalDateTime.now(); }
}