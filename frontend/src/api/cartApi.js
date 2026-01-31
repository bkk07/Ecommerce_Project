import axios from 'axios';

const API_BASE_URL = 'http://localhost:8080/api/v1/cart';

// Create axios instance with auth interceptor
const cartClient = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Add token to all requests
cartClient.interceptors.request.use(
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
 * Get user's cart
 * @returns {Promise<{userId: string, items: Array, totalAmount: number}>}
 */
export const getCart = async () => {
  const response = await cartClient.get('');
  return response.data;
};

/**
 * Add item to cart
 * @param {string} skuCode - Product SKU
 * @param {number} quantity - Quantity to add
 * @param {string} imageUrl - Product image URL
 * @param {number} price - Product price
 */
export const addToCart = async (skuCode, quantity = 1, imageUrl = '', price = 0) => {
  const response = await cartClient.post('/add', { skuCode, quantity, imageUrl, price });
  return response.data;
};

/**
 * Update item price in cart
 * @param {string} skuCode - Product SKU
 * @param {number} newPrice - New price to update
 */
export const updateCartItemPrice = async (skuCode, newPrice) => {
  const response = await cartClient.put(`/update-price/${encodeURIComponent(skuCode)}`, { price: newPrice });
  return response.data;
};

/**
 * Remove item from cart
 * @param {string} skuCode - Product SKU to remove
 */
export const removeFromCart = async (skuCode) => {
  const response = await cartClient.delete(`/remove/${encodeURIComponent(skuCode)}`);
  return response.data;
};

/**
 * Clear entire cart
 */
export const clearCart = async () => {
  const response = await cartClient.delete('/clear');
  return response.data;
};

export default cartClient;
