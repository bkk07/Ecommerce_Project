import apiClient from './apiClient';

const API_BASE_PATH = '/api/v1/wishlist';

/**
 * Get user's wishlist
 * @returns {Promise<{id: number, userId: number, items: Array, totalItems: number}>}
 */
export const getWishlist = async () => {
  const response = await apiClient.get(API_BASE_PATH);
  return response.data;
};

/**
 * Add item to wishlist
 * @param {string} skuCode - Product SKU
 * @param {string} name - Product name
 * @param {number} price - Product price
 * @param {string} imageUrl - Product image URL
 * @param {number} productId - Product ID (optional)
 */
export const addToWishlist = async (skuCode, name, price, imageUrl = '', productId = null) => {
  const response = await apiClient.post(`${API_BASE_PATH}/items`, { 
    skuCode, 
    name, 
    price, 
    imageUrl,
    productId 
  });
  return response.data;
};

/**
 * Remove item from wishlist
 * @param {string} skuCode - Product SKU to remove
 */
export const removeFromWishlist = async (skuCode) => {
  const response = await apiClient.delete(`${API_BASE_PATH}/items/${encodeURIComponent(skuCode)}`);
  return response.data;
};

/**
 * Check if item exists in wishlist
 * @param {string} skuCode - Product SKU to check
 * @returns {Promise<{skuCode: string, exists: boolean}>}
 */
export const checkItemInWishlist = async (skuCode) => {
  const response = await apiClient.get(`${API_BASE_PATH}/items/${encodeURIComponent(skuCode)}/exists`);
  return response.data;
};

/**
 * Move item from wishlist to cart
 * @param {string} skuCode - Product SKU to move
 */
export const moveToCart = async (skuCode) => {
  const response = await apiClient.post(`${API_BASE_PATH}/${encodeURIComponent(skuCode)}/move-to-cart`);
  return response.data;
};

/**
 * Clear all items from wishlist
 */
export const clearWishlist = async () => {
  const response = await apiClient.delete(`${API_BASE_PATH}/clear`);
  return response.data;
};

export default {
  getWishlist,
  addToWishlist,
  removeFromWishlist,
  checkItemInWishlist,
  moveToCart,
  clearWishlist,
};
