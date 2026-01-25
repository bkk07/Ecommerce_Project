package com.ecommerce.checkout;

public class OrderCheckoutResponse {
    String razorpayOrderId;
    public OrderCheckoutResponse(){}
    public OrderCheckoutResponse(String razorpayOrderId) {
        this.razorpayOrderId = razorpayOrderId;
    }
    public String getRazorpayOrderId() {
        return razorpayOrderId;
    }
    public void setRazorpayOrderId(String razorpayOrderId) {
        this.razorpayOrderId = razorpayOrderId;
    }
}
