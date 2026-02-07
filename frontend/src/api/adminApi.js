import apiClient from './apiClient';

const ADMIN_BASE_PATH = '/admin';

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
  const response = await apiClient.get(`${ADMIN_BASE_PATH}/users/stats`);
  return response.data;
};
