package com.ecommerce.checkoutservice.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

public class Exceptions {
    
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public static class PaymentFailedException extends RuntimeException {
        public PaymentFailedException(String message) { 
            super(message); 
        }
    }
    
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public static class SessionNotFoundException extends RuntimeException {
        public SessionNotFoundException(String message) { 
            super(message); 
        }
    }
    
    @ResponseStatus(HttpStatus.CONFLICT)
    public static class DuplicateCheckoutException extends RuntimeException {
        public DuplicateCheckoutException(String message) { 
            super(message); 
        }
    }
    
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public static class ServiceUnavailableException extends RuntimeException {
        public ServiceUnavailableException(String message) { 
            super(message); 
        }
    }
    
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public static class InvalidCheckoutStateException extends RuntimeException {
        public InvalidCheckoutStateException(String message) { 
            super(message); 
        }
    }
}
