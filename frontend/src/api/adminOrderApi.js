import apiClient from './apiClient';

const ADMIN_ORDERS_BASE_PATH = '/admin/orders';

/**
 * Fetch order statistics for admin dashboard
 */
export const fetchOrderStats = async () => {
  const response = await apiClient.get(`${ADMIN_ORDERS_BASE_PATH}/stats`);
  return response.data;
};

/**
 * Fetch paginated list of all orders
 */
export const fetchAllOrders = async (page = 0, size = 10, sortBy = "createdAt", sortDirection = "desc") => {
  const response = await apiClient.get(ADMIN_ORDERS_BASE_PATH, {
    params: { page, size, sortBy, sortDirection },
  });
  return response.data;
};

/**
 * Fetch orders filtered by status
 */
export const fetchOrdersByStatus = async (status, page = 0, size = 10) => {
  const response = await apiClient.get(`${ADMIN_ORDERS_BASE_PATH}/status/${status}`, {
    params: { page, size },
  });
  return response.data;
};

/**
 * Fetch orders within a date range
 */
export const fetchOrdersByDateRange = async (startDate, endDate, page = 0, size = 10) => {
  const response = await apiClient.get(`${ADMIN_ORDERS_BASE_PATH}/date-range`, {
    params: { startDate, endDate, page, size },
  });
  return response.data;
};

/**
 * Search orders by orderId or userId
 */
export const searchOrders = async (keyword, page = 0, size = 10) => {
  const response = await apiClient.get(`${ADMIN_ORDERS_BASE_PATH}/search`, {
    params: { keyword, page, size },
  });
  return response.data;
};

/**
 * Fetch detailed order by ID
 */
export const fetchOrderById = async (orderId) => {
  const response = await apiClient.get(`${ADMIN_ORDERS_BASE_PATH}/${orderId}`);
  return response.data;
};
