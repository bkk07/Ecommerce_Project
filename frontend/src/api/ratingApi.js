import axios from 'axios';

const API_BASE_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080';

const ratingClient = axios.create({
  baseURL: `${API_BASE_URL}/api/v1/ratings`,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Add auth token to requests
ratingClient.interceptors.request.use((config) => {
  const token = localStorage.getItem('token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

/**
 * Create a new rating for a product
 * @param {Object} ratingData - { sku, orderId, rating, message }
 */
export const createRating = async (ratingData) => {
  const response = await ratingClient.post('', ratingData);
  return response.data;
};

/**
 * Update an existing rating
 * @param {number} ratingId - Rating ID
 * @param {Object} updateData - { rating, message }
 */
export const updateRating = async (ratingId, updateData) => {
  const response = await ratingClient.put(`/${ratingId}`, updateData);
  return response.data;
};

/**
 * Delete a rating
 * @param {number} ratingId - Rating ID
 */
export const deleteRating = async (ratingId) => {
  await ratingClient.delete(`/${ratingId}`);
};

/**
 * Get rating by ID
 * @param {number} ratingId - Rating ID
 */
export const getRatingById = async (ratingId) => {
  const response = await ratingClient.get(`/${ratingId}`);
  return response.data;
};

/**
 * Get user's rating for a specific order and SKU
 * @param {string} orderId - Order ID
 * @param {string} sku - Product SKU
 */
export const getUserRatingForOrderAndSku = async (orderId, sku) => {
  try {
    const response = await ratingClient.get(`/order/${orderId}/sku/${sku}`);
    return response.data;
  } catch (error) {
    if (error.response?.status === 404) {
      return null;
    }
    throw error;
  }
};

/**
 * Get all ratings by current user
 */
export const getMyRatings = async () => {
  const response = await ratingClient.get('/my-ratings');
  return response.data;
};

/**
 * Get user's ratings for a specific order
 * @param {string} orderId - Order ID
 */
export const getUserRatingsForOrder = async (orderId) => {
  const response = await ratingClient.get(`/order/${orderId}`);
  return response.data;
};

/**
 * Get all approved ratings for a product (public)
 * @param {string} sku - Product SKU
 */
export const getProductRatings = async (sku) => {
  const response = await ratingClient.get(`/product/${sku}`);
  return response.data;
};

/**
 * Get rating summary for a product (public)
 * @param {string} sku - Product SKU
 */
export const getProductRatingSummary = async (sku) => {
  const response = await ratingClient.get(`/product/${sku}/summary`);
  return response.data;
};

/**
 * Check if user has rated a product in an order
 * @param {string} orderId - Order ID
 * @param {string} sku - Product SKU
 */
export const hasUserRatedProduct = async (orderId, sku) => {
  const response = await ratingClient.get('/check', {
    params: { orderId, sku }
  });
  return response.data;
};

export default ratingClient;
