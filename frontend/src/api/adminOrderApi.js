import axios from "axios";

const API_BASE_URL = "http://localhost:8080/admin/orders";

// Get auth header with token
const getAuthHeaders = () => {
  const token = localStorage.getItem("token");
  return {
    Authorization: `Bearer ${token}`,
    "Content-Type": "application/json",
  };
};

/**
 * Fetch order statistics for admin dashboard
 */
export const fetchOrderStats = async () => {
  const response = await axios.get(`${API_BASE_URL}/stats`, {
    headers: getAuthHeaders(),
  });
  return response.data;
};

/**
 * Fetch paginated list of all orders
 */
export const fetchAllOrders = async (page = 0, size = 10, sortBy = "createdAt", sortDirection = "desc") => {
  const response = await axios.get(API_BASE_URL, {
    headers: getAuthHeaders(),
    params: { page, size, sortBy, sortDirection },
  });
  return response.data;
};

/**
 * Fetch orders filtered by status
 */
export const fetchOrdersByStatus = async (status, page = 0, size = 10) => {
  const response = await axios.get(`${API_BASE_URL}/status/${status}`, {
    headers: getAuthHeaders(),
    params: { page, size },
  });
  return response.data;
};

/**
 * Fetch orders within a date range
 */
export const fetchOrdersByDateRange = async (startDate, endDate, page = 0, size = 10) => {
  const response = await axios.get(`${API_BASE_URL}/date-range`, {
    headers: getAuthHeaders(),
    params: { startDate, endDate, page, size },
  });
  return response.data;
};

/**
 * Search orders by orderId or userId
 */
export const searchOrders = async (keyword, page = 0, size = 10) => {
  const response = await axios.get(`${API_BASE_URL}/search`, {
    headers: getAuthHeaders(),
    params: { keyword, page, size },
  });
  return response.data;
};

/**
 * Fetch detailed order by ID
 */
export const fetchOrderById = async (orderId) => {
  const response = await axios.get(`${API_BASE_URL}/${orderId}`, {
    headers: getAuthHeaders(),
  });
  return response.data;
};
