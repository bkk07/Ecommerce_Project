package com.ecommerce.wishlistservice.exception;

public class WishlistNotFoundException extends RuntimeException {

    public WishlistNotFoundException(String message) {
        super(message);
    }

    public WishlistNotFoundException(Long userId) {
        super("Wishlist not found for user ID: " + userId);
    }
}
