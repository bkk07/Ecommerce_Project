import axios from 'axios';

const API_BASE_URL = 'http://localhost:8080/auth';

const authClient = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

/**
 * Register a new user
 * @param {Object} userData - User registration data
 * @param {string} userData.name - User's full name
 * @param {string} userData.email - User's email
 * @param {string} userData.password - User's password
 * @param {string} userData.phone - User's phone number
 * @returns {Promise<{userId: number, role: string}>}
 */
export const registerUser = async (userData) => {
  const response = await authClient.post('/register', userData);
  return response.data;
};

/**
 * Login user
 * @param {Object} credentials - Login credentials
 * @param {string} credentials.email - User's email
 * @param {string} credentials.password - User's password
 * @returns {Promise<{token: string, userId: number, role: string}>}
 */
export const loginUser = async (credentials) => {
  const response = await authClient.post('/login', credentials);
  return response.data;
};

/**
 * Send email verification OTP
 * @param {number} userId - User's ID
 */
export const sendEmailVerificationOtp = async (userId) => {
  const response = await authClient.post(`/verify-email/send-otp?userId=${userId}`);
  return response.data;
};

/**
 * Verify email with OTP
 * @param {number} userId - User's ID
 * @param {string} otp - OTP code
 * @returns {Promise<{token: string, userId: number, role: string}>}
 */
export const verifyEmailOtp = async (userId, otp) => {
  const response = await authClient.post(`/verify-email?userId=${userId}&otp=${otp}`);
  return response.data;
};

/**
 * Send forgot password OTP
 * @param {string} email - User's email
 */
export const sendForgotPasswordOtp = async (email) => {
  const response = await authClient.post(`/forgot-password/send-otp?email=${encodeURIComponent(email)}`);
  return response.data;
};

/**
 * Verify forgot password OTP
 * @param {string} email - User's email
 * @param {string} otp - OTP code
 */
export const verifyForgotPasswordOtp = async (email, otp) => {
  const response = await authClient.post(`/forgot-password/verify-otp?email=${encodeURIComponent(email)}&otp=${otp}`);
  return response.data;
};

/**
 * Reset password after OTP verification
 * @param {Object} data - Password reset data
 * @param {string} data.email - User's email
 * @param {string} data.newPassword - New password
 */
export const resetPassword = async (data) => {
  const response = await authClient.post('/forgot-password', data);
  return response.data;
};

export default authClient;
