import axios from 'axios';

const API_BASE_URL = 'http://localhost:8080/api/v1/wishlist';

// Create axios instance with auth interceptor
const wishlistClient = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Add token to all requests
wishlistClient.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

/**
 * Get user's wishlist
 * @returns {Promise<{id: number, userId: number, items: Array, totalItems: number}>}
 */
export const getWishlist = async () => {
  const response = await wishlistClient.get('');
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
  const response = await wishlistClient.post('/items', { 
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
  const response = await wishlistClient.delete(`/items/${encodeURIComponent(skuCode)}`);
  return response.data;
};

/**
 * Check if item exists in wishlist
 * @param {string} skuCode - Product SKU to check
 * @returns {Promise<{skuCode: string, exists: boolean}>}
 */
export const checkItemInWishlist = async (skuCode) => {
  const response = await wishlistClient.get(`/items/${encodeURIComponent(skuCode)}/exists`);
  return response.data;
};

/**
 * Move item from wishlist to cart
 * @param {string} skuCode - Product SKU to move
 */
export const moveToCart = async (skuCode) => {
  const response = await wishlistClient.post(`/${encodeURIComponent(skuCode)}/move-to-cart`);
  return response.data;
};

/**
 * Clear all items from wishlist
 */
export const clearWishlist = async () => {
  const response = await wishlistClient.delete('/clear');
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
