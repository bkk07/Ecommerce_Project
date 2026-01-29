package com.ecommerce.payment;
public class PaymentRefundedEvent {
    private String orderId;
    private String paymentId;
    private String refundId;

    public PaymentRefundedEvent() {
    }

    public PaymentRefundedEvent(String orderId, String paymentId, String refundId) {
        this.orderId = orderId;
        this.paymentId = paymentId;
        this.refundId = refundId;
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public String getPaymentId() {
        return paymentId;
    }

    public void setPaymentId(String paymentId) {
        this.paymentId = paymentId;
    }

    public String getRefundId() {
        return refundId;
    }

    public void setRefundId(String refundId) {
        this.refundId = refundId;
    }
}
