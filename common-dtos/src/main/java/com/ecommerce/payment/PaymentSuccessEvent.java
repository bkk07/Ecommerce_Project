package com.ecommerce.payment;
public class PaymentSuccessEvent {
    private String orderId;
    private String paymentId;
    private String paymentMethod;
    public PaymentSuccessEvent() {

    }
    public PaymentSuccessEvent(String orderId, String paymentId, String paymentMethod) {
        this.orderId = orderId;
        this.paymentId = paymentId;
        this.paymentMethod = paymentMethod;
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

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }
}