import axios from 'axios';

const API_BASE_URL = 'http://localhost:8080/api/orders';

// Create axios instance with auth interceptor
const orderClient = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Add token to all requests
orderClient.interceptors.request.use(
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
 * Fetch all orders for a logged-in user
 * @param {string} userId - The user ID
 * @returns {Promise<Array<{orderNumber: string, status: string, totalAmount: number, orderDate: string, shippingAddress: string, items: Array}>>}
 */
export const getUserOrders = async (userId) => {
  const response = await orderClient.get(`/user/${userId}`);
  return response.data;
};

/**
 * Get details of a specific order
 * @param {string} orderNumber - The order number
 * @returns {Promise<{orderNumber: string, status: string, totalAmount: number, orderDate: string, shippingAddress: string, items: Array}>}
 */
export const getOrderDetails = async (orderNumber) => {
  const response = await orderClient.get(`/${orderNumber}`);
  return response.data;
};

export default orderClient;
