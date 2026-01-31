package com.ecommerce.wishlistservice.exception;

public class WishlistItemNotFoundException extends RuntimeException {

    public WishlistItemNotFoundException(String message) {
        super(message);
    }

    public WishlistItemNotFoundException(String skuCode, Long userId) {
        super("Item with SKU '" + skuCode + "' not found in wishlist for user ID: " + userId);
    }
}
