import axios from 'axios';

const API_BASE_URL = 'http://localhost:8080/admin';

/**
 * Create axios instance for admin API calls
 * Includes JWT token in Authorization header
 */
const createAdminClient = () => {
  const token = localStorage.getItem('token');
  return axios.create({
    baseURL: API_BASE_URL,
    headers: {
      'Content-Type': 'application/json',
      ...(token && { Authorization: `Bearer ${token}` }),
    },
  });
};

/**
 * Get user statistics for admin dashboard
 * @returns {Promise<{
 *   totalUsers: number,
 *   adminCount: number,
 *   userCount: number,
 *   emailVerified: number,
 *   phoneVerified: number,
 *   newUsers: {
 *     today: number,
 *     thisWeek: number,
 *     thisMonth: number,
 *     thisYear: number
 *   }
 * }>}
 */
export const getUserStats = async () => {
  const client = createAdminClient();
  const response = await client.get('/users/stats');
  return response.data;
};
