package com.ecommerce.checkoutservice.exception;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;
public class Exceptions {
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public static class PaymentFailedException extends RuntimeException {
        public PaymentFailedException(String message) { super(message); }
    }
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public static class SessionNotFoundException extends RuntimeException {
        public SessionNotFoundException(String message) { super(message); }
    }
}
