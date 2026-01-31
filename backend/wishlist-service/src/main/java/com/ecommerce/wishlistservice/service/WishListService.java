package com.ecommerce.wishlistservice.service;

import com.ecommerce.wishlistservice.dto.request.AddWishListItemRequest;
import com.ecommerce.wishlistservice.dto.response.ItemExistsResponse;
import com.ecommerce.wishlistservice.dto.response.WishListItemResponse;
import com.ecommerce.wishlistservice.dto.response.WishListResponse;

public interface WishListService {

    /**
     * Get wishlist for the logged-in user
     * @param userId User ID from JWT
     * @return WishListResponse containing all wishlist items
     */
    WishListResponse getWishList(Long userId);

    /**
     * Add item to wishlist by SKU code
     * @param userId User ID from JWT
     * @param request Add item request containing SKU and product details
     * @return Added WishListItemResponse
     */
    WishListItemResponse addItem(Long userId, AddWishListItemRequest request);

    /**
     * Remove item from wishlist by SKU code
     * @param userId User ID from JWT
     * @param skuCode SKU code of the item to remove
     */
    void removeItem(Long userId, String skuCode);

    /**
     * Check if item exists in wishlist by SKU code
     * @param userId User ID from JWT
     * @param skuCode SKU code to check
     * @return ItemExistsResponse indicating whether item exists
     */
    ItemExistsResponse itemExists(Long userId, String skuCode);

    /**
     * Move item from wishlist to cart
     * @param userId User ID from JWT
     * @param skuCode SKU code of the item to move
     * @return WishListItemResponse of the moved item
     */
    WishListItemResponse moveToCart(Long userId, String skuCode);

    /**
     * Clear all items from wishlist
     * @param userId User ID from JWT
     */
    void clearWishList(Long userId);
}
