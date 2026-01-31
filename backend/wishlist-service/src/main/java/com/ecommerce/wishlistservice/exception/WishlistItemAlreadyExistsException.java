package com.ecommerce.wishlistservice.exception;

public class WishlistItemAlreadyExistsException extends RuntimeException {

    public WishlistItemAlreadyExistsException(String message) {
        super(message);
    }

    public WishlistItemAlreadyExistsException(String skuCode, Long userId) {
        super("Item with SKU '" + skuCode + "' already exists in wishlist for user ID: " + userId);
    }
}
