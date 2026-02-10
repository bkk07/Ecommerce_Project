import axios from 'axios';
import { refreshAccessToken } from './authApi';

const API_BASE_URL = 'http://localhost:8080';

// Create the main API client
const apiClient = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Flag to prevent multiple refresh requests
let isRefreshing = false;
let failedQueue = [];

const processQueue = (error, token = null) => {
  failedQueue.forEach(prom => {
    if (error) {
      prom.reject(error);
    } else {
      prom.resolve(token);
    }
  });
  failedQueue = [];
};

// Helper to get stored tokens
const getStoredTokens = () => ({
  accessToken: localStorage.getItem('token'),
  refreshToken: localStorage.getItem('refreshToken'),
});

// Helper to store tokens
const storeTokens = (accessToken, refreshToken, expiresIn) => {
  localStorage.setItem('token', accessToken);
  if (refreshToken) {
    localStorage.setItem('refreshToken', refreshToken);
  }
  if (expiresIn) {
    const expiryTime = Date.now() + expiresIn;
    localStorage.setItem('tokenExpiry', expiryTime.toString());
  }
};

// Helper to clear auth data
const clearAuth = () => {
  localStorage.removeItem('token');
  localStorage.removeItem('refreshToken');
  localStorage.removeItem('tokenExpiry');
  localStorage.removeItem('userId');
  localStorage.removeItem('role');
};

// Check if token is about to expire (within 1 minute)
const isTokenExpiringSoon = () => {
  const expiry = localStorage.getItem('tokenExpiry');
  if (!expiry) return true;
  const expiryTime = parseInt(expiry, 10);
  return Date.now() > expiryTime - 60000; // 1 minute buffer
};

// Request interceptor - add auth token
apiClient.interceptors.request.use(
  async (config) => {
    // Skip auth for public endpoints
    const publicEndpoints = ['/auth/login', '/auth/register', '/auth/refresh', '/auth/forgot-password'];
    const isPublicEndpoint = publicEndpoints.some(endpoint => config.url?.includes(endpoint));
    
    if (isPublicEndpoint) {
      return config;
    }

    const { accessToken, refreshToken } = getStoredTokens();
    
    // If token is expiring soon and we have a refresh token, proactively refresh
    if (accessToken && refreshToken && isTokenExpiringSoon() && !isRefreshing) {
      isRefreshing = true;
      try {
        const response = await refreshAccessToken(refreshToken);
        storeTokens(response.token, response.refreshToken, response.expiresIn);
        config.headers.Authorization = `Bearer ${response.token}`;
        isRefreshing = false;
      } catch (error) {
        isRefreshing = false;
        // If refresh fails, continue with old token - response interceptor will handle 401
      }
    } else if (accessToken) {
      config.headers.Authorization = `Bearer ${accessToken}`;
    }
    
    return config;
  },
  (error) => Promise.reject(error)
);

// Response interceptor - handle 401 and refresh token
apiClient.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config;
    
    // If error is not 401 or request already retried, reject
    if (error.response?.status !== 401 || originalRequest._retry) {
      return Promise.reject(error);
    }
    
    // Skip refresh for auth endpoints
    if (originalRequest.url?.includes('/auth/')) {
      return Promise.reject(error);
    }
    
    // If we're already refreshing, queue this request
    if (isRefreshing) {
      return new Promise((resolve, reject) => {
        failedQueue.push({ resolve, reject });
      })
        .then(token => {
          originalRequest.headers.Authorization = `Bearer ${token}`;
          return apiClient(originalRequest);
        })
        .catch(err => Promise.reject(err));
    }
    
    originalRequest._retry = true;
    isRefreshing = true;
    
    const { refreshToken } = getStoredTokens();
    
    if (!refreshToken) {
      isRefreshing = false;
      clearAuth();
      // Dispatch logout event for Redux to handle
      window.dispatchEvent(new CustomEvent('auth:logout'));
      return Promise.reject(error);
    }
    
    try {
      const response = await refreshAccessToken(refreshToken);
      
      storeTokens(response.token, response.refreshToken, response.expiresIn);
      
      // Update auth header for original request
      originalRequest.headers.Authorization = `Bearer ${response.token}`;
      
      // Process queued requests
      processQueue(null, response.token);
      
      isRefreshing = false;
      
      // Retry original request
      return apiClient(originalRequest);
    } catch (refreshError) {
      processQueue(refreshError, null);
      isRefreshing = false;
      clearAuth();
      // Dispatch logout event for Redux to handle
      window.dispatchEvent(new CustomEvent('auth:logout'));
      return Promise.reject(refreshError);
    }
  }
);

// Export helper functions for use in other modules
export { getStoredTokens, storeTokens, clearAuth, isTokenExpiringSoon };

export default apiClient;
