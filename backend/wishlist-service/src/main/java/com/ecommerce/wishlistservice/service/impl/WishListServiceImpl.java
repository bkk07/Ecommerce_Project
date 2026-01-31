package com.ecommerce.wishlistservice.service.impl;

import com.ecommerce.wishlistservice.dto.request.AddWishListItemRequest;
import com.ecommerce.wishlistservice.dto.response.ItemExistsResponse;
import com.ecommerce.wishlistservice.dto.response.WishListItemResponse;
import com.ecommerce.wishlistservice.dto.response.WishListResponse;
import com.ecommerce.wishlistservice.entity.WishList;
import com.ecommerce.wishlistservice.entity.WishListItem;
import com.ecommerce.wishlistservice.exception.WishlistItemAlreadyExistsException;
import com.ecommerce.wishlistservice.exception.WishlistItemNotFoundException;
import com.ecommerce.wishlistservice.exception.WishlistNotFoundException;
import com.ecommerce.wishlistservice.repository.WishListItemRepository;
import com.ecommerce.wishlistservice.repository.WishListRepository;
import com.ecommerce.wishlistservice.service.WishListService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class WishListServiceImpl implements WishListService {

    private static final String WISHLIST_CACHE = "wishlist";

    private final WishListRepository wishListRepository;
    private final WishListItemRepository wishListItemRepository;

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = WISHLIST_CACHE, key = "#userId")
    public WishListResponse getWishList(Long userId) {
        log.info("Fetching wishlist for user: {}", userId);
        
        return wishListRepository.findByUserIdWithItems(userId)
                .map(this::mapToWishListResponse)
                .orElseGet(() -> createEmptyWishListResponse(userId));
    }

    @Override
    @Transactional
    @CacheEvict(value = WISHLIST_CACHE, key = "#userId")
    public WishListItemResponse addItem(Long userId, AddWishListItemRequest request) {
        log.info("Adding item with SKU: {} to wishlist for user: {}", request.getSkuCode(), userId);

        // Check if item already exists
        if (wishListItemRepository.existsByUserIdAndSkuCode(userId, request.getSkuCode())) {
            throw new WishlistItemAlreadyExistsException(request.getSkuCode(), userId);
        }

        // Get or create wishlist for user
        WishList wishList = wishListRepository.findByUserId(userId)
                .orElseGet(() -> createNewWishList(userId));

        // Create and add new item
        WishListItem item = WishListItem.builder()
                .skuCode(request.getSkuCode())
                .productId(request.getProductId())
                .name(request.getName())
                .imageUrl(request.getImageUrl())
                .price(request.getPrice())
                .build();

        wishList.addItem(item);
        wishListRepository.save(wishList);

        log.info("Item added successfully to wishlist for user: {}", userId);
        return mapToWishListItemResponse(item);
    }

    @Override
    @Transactional
    @CacheEvict(value = WISHLIST_CACHE, key = "#userId")
    public void removeItem(Long userId, String skuCode) {
        log.info("Removing item with SKU: {} from wishlist for user: {}", skuCode, userId);

        WishListItem item = wishListItemRepository.findByUserIdAndSkuCode(userId, skuCode)
                .orElseThrow(() -> new WishlistItemNotFoundException(skuCode, userId));

        WishList wishList = item.getWishList();
        wishList.removeItem(item);
        wishListItemRepository.delete(item);

        log.info("Item removed successfully from wishlist for user: {}", userId);
    }

    @Override
    @Transactional(readOnly = true)
    public ItemExistsResponse itemExists(Long userId, String skuCode) {
        log.info("Checking if item with SKU: {} exists in wishlist for user: {}", skuCode, userId);
        
        boolean exists = wishListItemRepository.existsByUserIdAndSkuCode(userId, skuCode);
        
        return ItemExistsResponse.builder()
                .skuCode(skuCode)
                .exists(exists)
                .build();
    }

    @Override
    @Transactional
    @CacheEvict(value = WISHLIST_CACHE, key = "#userId")
    public WishListItemResponse moveToCart(Long userId, String skuCode) {
        log.info("Moving item with SKU: {} from wishlist to cart for user: {}", skuCode, userId);

        // Find the item in wishlist
        WishListItem item = wishListItemRepository.findByUserIdAndSkuCode(userId, skuCode)
                .orElseThrow(() -> new WishlistItemNotFoundException(skuCode, userId));

        // Map to response before removing
        WishListItemResponse response = mapToWishListItemResponse(item);

        // Remove from wishlist
        WishList wishList = item.getWishList();
        wishList.removeItem(item);
        wishListItemRepository.delete(item);

        // Note: The actual cart addition will be handled by the frontend
        // which will call the cart-service endpoint with the item details
        // This service only removes the item from wishlist

        log.info("Item moved from wishlist for user: {}. Frontend should now add to cart.", userId);
        return response;
    }

    @Override
    @Transactional
    @CacheEvict(value = WISHLIST_CACHE, key = "#userId")
    public void clearWishList(Long userId) {
        log.info("Clearing wishlist for user: {}", userId);

        WishList wishList = wishListRepository.findByUserId(userId)
                .orElseThrow(() -> new WishlistNotFoundException(userId));

        wishList.getItems().clear();
        wishListRepository.save(wishList);

        log.info("Wishlist cleared successfully for user: {}", userId);
    }

    private WishList createNewWishList(Long userId) {
        log.info("Creating new wishlist for user: {}", userId);
        WishList wishList = WishList.builder()
                .userId(userId)
                .build();
        return wishListRepository.save(wishList);
    }

    private WishListResponse createEmptyWishListResponse(Long userId) {
        return WishListResponse.builder()
                .userId(userId)
                .items(Collections.emptyList())
                .totalItems(0)
                .build();
    }

    private WishListResponse mapToWishListResponse(WishList wishList) {
        List<WishListItemResponse> items = wishList.getItems().stream()
                .map(this::mapToWishListItemResponse)
                .collect(Collectors.toList());

        return WishListResponse.builder()
                .id(wishList.getId())
                .userId(wishList.getUserId())
                .createdAt(wishList.getCreatedAt())
                .items(items)
                .totalItems(items.size())
                .build();
    }

    private WishListItemResponse mapToWishListItemResponse(WishListItem item) {
        return WishListItemResponse.builder()
                .id(item.getId())
                .skuCode(item.getSkuCode())
                .productId(item.getProductId())
                .name(item.getName())
                .imageUrl(item.getImageUrl())
                .price(item.getPrice())
                .createdAt(item.getCreatedAt())
                .build();
    }
}
