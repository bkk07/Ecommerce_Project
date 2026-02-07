import apiClient from './apiClient';

const API_BASE_PATH = '/api/orders';

/**
 * Fetch all orders for a logged-in user
 * @param {string} userId - The user ID
 * @returns {Promise<Array<{orderNumber: string, status: string, totalAmount: number, orderDate: string, shippingAddress: string, items: Array}>>}
 */
export const getUserOrders = async (userId) => {
  const response = await apiClient.get(`${API_BASE_PATH}/user/${userId}`);
  return response.data;
};

/**
 * Get details of a specific order
 * @param {string} orderNumber - The order number
 * @returns {Promise<{orderNumber: string, status: string, totalAmount: number, orderDate: string, shippingAddress: string, items: Array}>}
 */
export const getOrderDetails = async (orderNumber) => {
  const response = await apiClient.get(`${API_BASE_PATH}/${orderNumber}`);
  return response.data;
};

export default apiClient;
