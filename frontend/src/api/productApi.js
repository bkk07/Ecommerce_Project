import axios from 'axios';

const API_BASE_URL = 'http://localhost:8080/api/v1';

const apiClient = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

export const fetchProductsByCategory = async (category = 'Smartphones') => {
  const response = await apiClient.get(`/search?category=${category}`);
  return response.data;
};

export const searchProductsByKeyword = async (keyword) => {
  const response = await apiClient.get(`/search?keyword=${encodeURIComponent(keyword)}`);
  return response.data;
};

/**
 * Fetch product details by SKU
 * @param {string} sku - Product SKU
 * @returns {Promise<ProductSkuResponse>}
 */
export const fetchProductBySku = async (sku) => {
  const response = await apiClient.get(`/products/sku/${encodeURIComponent(sku)}`);
  return response.data;
};

/**
 * Advanced search with filters and sorting
 * @param {Object} params - Search parameters
 * @param {string} params.keyword - Search keyword
 * @param {string} params.category - Category filter
 * @param {number} params.minPrice - Minimum price
 * @param {number} params.maxPrice - Maximum price
 * @param {string} params.sortBy - Sort field (price, name, createdAt)
 * @param {string} params.sortOrder - Sort order (asc, desc)
 * @param {number} params.page - Page number
 * @param {number} params.size - Page size
 */
export const searchProductsWithFilters = async (params) => {
  const queryParams = new URLSearchParams();
  
  if (params.keyword) queryParams.append('keyword', params.keyword);
  if (params.category) queryParams.append('category', params.category);
  if (params.minPrice !== undefined && params.minPrice !== null && params.minPrice !== '') {
    queryParams.append('minPrice', params.minPrice);
  }
  if (params.maxPrice !== undefined && params.maxPrice !== null && params.maxPrice !== '') {
    queryParams.append('maxPrice', params.maxPrice);
  }
  if (params.sortBy) {
    queryParams.append('sort', `${params.sortBy},${params.sortOrder || 'asc'}`);
  }
  if (params.page !== undefined) queryParams.append('page', params.page);
  if (params.size !== undefined) queryParams.append('size', params.size);
  
  const response = await apiClient.get(`/search?${queryParams.toString()}`);
  return response.data;
};

export default apiClient;
