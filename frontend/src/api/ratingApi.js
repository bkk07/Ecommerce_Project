import apiClient from './apiClient';

const API_BASE_PATH = '/api/v1/ratings';

/**
 * Create a new rating for a product
 * @param {Object} ratingData - { sku, orderId, rating, message }
 */
export const createRating = async (ratingData) => {
  const response = await apiClient.post(API_BASE_PATH, ratingData);
  return response.data;
};

/**
 * Update an existing rating
 * @param {number} ratingId - Rating ID
 * @param {Object} updateData - { rating, message }
 */
export const updateRating = async (ratingId, updateData) => {
  const response = await apiClient.put(`${API_BASE_PATH}/${ratingId}`, updateData);
  return response.data;
};

/**
 * Delete a rating
 * @param {number} ratingId - Rating ID
 */
export const deleteRating = async (ratingId) => {
  await apiClient.delete(`${API_BASE_PATH}/${ratingId}`);
};

/**
 * Get rating by ID
 * @param {number} ratingId - Rating ID
 */
export const getRatingById = async (ratingId) => {
  const response = await apiClient.get(`${API_BASE_PATH}/${ratingId}`);
  return response.data;
};

/**
 * Get user's rating for a specific order and SKU
 * @param {string} orderId - Order ID
 * @param {string} sku - Product SKU
 */
export const getUserRatingForOrderAndSku = async (orderId, sku) => {
  try {
    const response = await apiClient.get(`${API_BASE_PATH}/order/${orderId}/sku/${sku}`);
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
  const response = await apiClient.get(`${API_BASE_PATH}/my-ratings`);
  return response.data;
};

/**
 * Get user's ratings for a specific order
 * @param {string} orderId - Order ID
 */
export const getUserRatingsForOrder = async (orderId) => {
  try {
    const response = await apiClient.get(`${API_BASE_PATH}/order/${orderId}`);
    return response.data;
  } catch (error) {
    if (error.response?.status === 404) {
      return []; // No ratings found for this order
    }
    throw error;
  }
};

/**
 * Get all approved ratings for a product (public)
 * @param {string} sku - Product SKU
 */
export const getProductRatings = async (sku) => {
  const response = await apiClient.get(`${API_BASE_PATH}/product/${sku}`);
  return response.data;
};

/**
 * Get rating summary for a product (public)
 * @param {string} sku - Product SKU
 */
export const getProductRatingSummary = async (sku) => {
  const response = await apiClient.get(`${API_BASE_PATH}/product/${sku}/summary`);
  return response.data;
};

/**
 * Check if user has rated a product in an order
 * @param {string} orderId - Order ID
 * @param {string} sku - Product SKU
 */
export const hasUserRatedProduct = async (orderId, sku) => {
  const response = await apiClient.get(`${API_BASE_PATH}/check`, {
    params: { orderId, sku }
  });
  return response.data;
};

export default apiClient;
