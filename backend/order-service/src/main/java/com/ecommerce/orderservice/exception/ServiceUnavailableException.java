package com.ecommerce.orderservice.exception;

/**
 * Exception thrown when an external service is unavailable
 */
public class ServiceUnavailableException extends RuntimeException {
    
    private final String serviceName;
    
    public ServiceUnavailableException(String serviceName) {
        super(serviceName + " is currently unavailable. Please try again later.");
        this.serviceName = serviceName;
    }
    
    public ServiceUnavailableException(String serviceName, Throwable cause) {
        super(serviceName + " is currently unavailable. Please try again later.", cause);
        this.serviceName = serviceName;
    }
    
    public String getServiceName() {
        return serviceName;
    }
}
