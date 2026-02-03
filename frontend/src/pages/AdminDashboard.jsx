import React, { useEffect } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import { useNavigate } from 'react-router-dom';
import { 
  fetchUserStats, 
  selectUserStats, 
  selectAdminLoading, 
  selectAdminError,
  selectLastFetched 
} from '../features/admin/adminSlice';
import { getOrderStats } from '../features/admin/adminOrderSlice';
import { selectUser, selectIsAuthenticated } from '../features/auth/authSlice';
import AdminLayout from '../components/AdminLayout';

const AdminDashboard = () => {
  const dispatch = useDispatch();
  const navigate = useNavigate();
  
  const user = useSelector(selectUser);
  const isAuthenticated = useSelector(selectIsAuthenticated);
  const userStats = useSelector(selectUserStats);
  const isLoading = useSelector(selectAdminLoading);
  const error = useSelector(selectAdminError);
  const lastFetched = useSelector(selectLastFetched);

  // Order stats from Redux
  const orderStats = useSelector((state) => state.adminOrders.stats);
  const orderStatsLoading = useSelector((state) => state.adminOrders.statsLoading);
  const orderStatsError = useSelector((state) => state.adminOrders.statsError);

  // Check if user is admin
  useEffect(() => {
    if (!isAuthenticated) {
      navigate('/login');
      return;
    }
    if (user?.role !== 'ADMIN') {
      navigate('/');
      return;
    }
  }, [isAuthenticated, user, navigate]);

  // Fetch stats on mount
  useEffect(() => {
    if (isAuthenticated && user?.role === 'ADMIN') {
      dispatch(fetchUserStats());
      dispatch(getOrderStats());
    }
  }, [dispatch, isAuthenticated, user]);

  // Refresh stats
  const handleRefresh = () => {
    dispatch(fetchUserStats());
    dispatch(getOrderStats());
  };

  // Format number with commas
  const formatNumber = (num) => {
    return num?.toLocaleString() || '0';
  };

  // Format currency
  const formatCurrency = (amount) => {
    if (!amount) return '$0.00';
    return new Intl.NumberFormat('en-US', { 
      style: 'currency', 
      currency: 'USD',
      minimumFractionDigits: 2 
    }).format(amount);
  };

  // Calculate percentage
  const calcPercentage = (part, total) => {
    if (!total || total === 0) return '0';
    return ((part / total) * 100).toFixed(1);
  };

  if (!isAuthenticated || user?.role !== 'ADMIN') {
    return null;
  }

  return (
    <AdminLayout>
      <div className="py-8">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          {/* Header */}
          <div className="mb-8">
            <div className="flex items-center justify-between">
              <div>
                <h1 className="text-3xl font-bold text-gray-900">User Statistics</h1>
                <p className="mt-1 text-sm text-gray-500">
                  User analytics and growth metrics
                </p>
              </div>
              <button
                onClick={handleRefresh}
                disabled={isLoading}
                className="inline-flex items-center px-4 py-2 border border-transparent rounded-lg shadow-sm text-sm font-medium text-white bg-purple-600 hover:bg-purple-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-purple-500 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
              >
                {isLoading ? (
                  <>
                    <svg className="animate-spin -ml-1 mr-2 h-4 w-4 text-white" fill="none" viewBox="0 0 24 24">
                      <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"></circle>
                      <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                    </svg>
                    Refreshing...
                  </>
                ) : (
                  <>
                    <svg className="w-4 h-4 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" />
                    </svg>
                    Refresh
                  </>
                )}
              </button>
            </div>
            {lastFetched && (
              <p className="mt-2 text-xs text-gray-400">
                Last updated: {new Date(lastFetched).toLocaleString()}
            </p>
          )}
        </div>

        {/* Error State */}
        {error && (
          <div className="mb-6 bg-red-50 border border-red-200 rounded-lg p-4">
            <div className="flex items-center">
              <svg className="w-5 h-5 text-red-400 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
              </svg>
              <span className="text-red-700">{error}</span>
            </div>
          </div>
        )}

        {/* Loading State */}
        {isLoading && !userStats && (
          <div className="flex items-center justify-center py-20">
            <div className="text-center">
              <svg className="animate-spin h-12 w-12 text-indigo-600 mx-auto mb-4" fill="none" viewBox="0 0 24 24">
                <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"></circle>
                <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
              </svg>
              <p className="text-gray-500">Loading statistics...</p>
            </div>
          </div>
        )}

        {/* Stats Content */}
        {userStats && (
          <>
            {/* Main KPI Cards */}
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6 mb-8">
              {/* Total Users */}
              <div className="bg-white rounded-xl shadow-sm border border-gray-100 p-6 hover:shadow-md transition-shadow">
                <div className="flex items-center justify-between">
                  <div>
                    <p className="text-sm font-medium text-gray-500">Total Users</p>
                    <p className="mt-2 text-3xl font-bold text-gray-900">{formatNumber(userStats.totalUsers)}</p>
                  </div>
                  <div className="p-3 bg-indigo-100 rounded-full">
                    <svg className="w-8 h-8 text-indigo-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 4.354a4 4 0 110 5.292M15 21H3v-1a6 6 0 0112 0v1zm0 0h6v-1a6 6 0 00-9-5.197M13 7a4 4 0 11-8 0 4 4 0 018 0z" />
                    </svg>
                  </div>
                </div>
              </div>

              {/* Regular Users */}
              <div className="bg-white rounded-xl shadow-sm border border-gray-100 p-6 hover:shadow-md transition-shadow">
                <div className="flex items-center justify-between">
                  <div>
                    <p className="text-sm font-medium text-gray-500">Regular Users</p>
                    <p className="mt-2 text-3xl font-bold text-gray-900">{formatNumber(userStats.userCount)}</p>
                    <p className="mt-1 text-xs text-gray-400">
                      {calcPercentage(userStats.userCount, userStats.totalUsers)}% of total
                    </p>
                  </div>
                  <div className="p-3 bg-green-100 rounded-full">
                    <svg className="w-8 h-8 text-green-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z" />
                    </svg>
                  </div>
                </div>
              </div>

              {/* Admins */}
              <div className="bg-white rounded-xl shadow-sm border border-gray-100 p-6 hover:shadow-md transition-shadow">
                <div className="flex items-center justify-between">
                  <div>
                    <p className="text-sm font-medium text-gray-500">Administrators</p>
                    <p className="mt-2 text-3xl font-bold text-gray-900">{formatNumber(userStats.adminCount)}</p>
                    <p className="mt-1 text-xs text-gray-400">
                      {calcPercentage(userStats.adminCount, userStats.totalUsers)}% of total
                    </p>
                  </div>
                  <div className="p-3 bg-purple-100 rounded-full">
                    <svg className="w-8 h-8 text-purple-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12l2 2 4-4m5.618-4.016A11.955 11.955 0 0112 2.944a11.955 11.955 0 01-8.618 3.04A12.02 12.02 0 003 9c0 5.591 3.824 10.29 9 11.622 5.176-1.332 9-6.03 9-11.622 0-1.042-.133-2.052-.382-3.016z" />
                    </svg>
                  </div>
                </div>
              </div>

              {/* New Users Today */}
              <div className="bg-white rounded-xl shadow-sm border border-gray-100 p-6 hover:shadow-md transition-shadow">
                <div className="flex items-center justify-between">
                  <div>
                    <p className="text-sm font-medium text-gray-500">New Today</p>
                    <p className="mt-2 text-3xl font-bold text-gray-900">{formatNumber(userStats.newUsers?.today)}</p>
                    <p className="mt-1 text-xs text-green-500 flex items-center">
                      <svg className="w-3 h-3 mr-1" fill="currentColor" viewBox="0 0 20 20">
                        <path fillRule="evenodd" d="M5.293 9.707a1 1 0 010-1.414l4-4a1 1 0 011.414 0l4 4a1 1 0 01-1.414 1.414L11 7.414V15a1 1 0 11-2 0V7.414L6.707 9.707a1 1 0 01-1.414 0z" clipRule="evenodd" />
                      </svg>
                      New registrations
                    </p>
                  </div>
                  <div className="p-3 bg-blue-100 rounded-full">
                    <svg className="w-8 h-8 text-blue-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M18 9v3m0 0v3m0-3h3m-3 0h-3m-2-5a4 4 0 11-8 0 4 4 0 018 0zM3 20a6 6 0 0112 0v1H3v-1z" />
                    </svg>
                  </div>
                </div>
              </div>
            </div>

            {/* Verification Stats */}
            <div className="grid grid-cols-1 lg:grid-cols-2 gap-6 mb-8">
              {/* Email Verification */}
              <div className="bg-white rounded-xl shadow-sm border border-gray-100 p-6">
                <h3 className="text-lg font-semibold text-gray-900 mb-4">Email Verification</h3>
                <div className="flex items-center justify-between mb-4">
                  <div>
                    <p className="text-2xl font-bold text-gray-900">{formatNumber(userStats.emailVerified)}</p>
                    <p className="text-sm text-gray-500">verified emails</p>
                  </div>
                  <div className="text-right">
                    <p className="text-2xl font-bold text-gray-400">{formatNumber(userStats.totalUsers - userStats.emailVerified)}</p>
                    <p className="text-sm text-gray-500">unverified</p>
                  </div>
                </div>
                <div className="w-full bg-gray-200 rounded-full h-3">
                  <div 
                    className="bg-gradient-to-r from-green-400 to-green-600 h-3 rounded-full transition-all duration-500"
                    style={{ width: `${calcPercentage(userStats.emailVerified, userStats.totalUsers)}%` }}
                  ></div>
                </div>
                <p className="mt-2 text-sm text-gray-500 text-center">
                  {calcPercentage(userStats.emailVerified, userStats.totalUsers)}% verified
                </p>
              </div>

              {/* Phone Verification */}
              <div className="bg-white rounded-xl shadow-sm border border-gray-100 p-6">
                <h3 className="text-lg font-semibold text-gray-900 mb-4">Phone Verification</h3>
                <div className="flex items-center justify-between mb-4">
                  <div>
                    <p className="text-2xl font-bold text-gray-900">{formatNumber(userStats.phoneVerified)}</p>
                    <p className="text-sm text-gray-500">verified phones</p>
                  </div>
                  <div className="text-right">
                    <p className="text-2xl font-bold text-gray-400">{formatNumber(userStats.totalUsers - userStats.phoneVerified)}</p>
                    <p className="text-sm text-gray-500">unverified</p>
                  </div>
                </div>
                <div className="w-full bg-gray-200 rounded-full h-3">
                  <div 
                    className="bg-gradient-to-r from-blue-400 to-blue-600 h-3 rounded-full transition-all duration-500"
                    style={{ width: `${calcPercentage(userStats.phoneVerified, userStats.totalUsers)}%` }}
                  ></div>
                </div>
                <p className="mt-2 text-sm text-gray-500 text-center">
                  {calcPercentage(userStats.phoneVerified, userStats.totalUsers)}% verified
                </p>
              </div>
            </div>

            {/* User Growth Stats */}
            <div className="bg-white rounded-xl shadow-sm border border-gray-100 p-6">
              <h3 className="text-lg font-semibold text-gray-900 mb-6">User Growth</h3>
              <div className="grid grid-cols-2 md:grid-cols-4 gap-6">
                {/* Today */}
                <div className="text-center p-4 bg-gradient-to-br from-blue-50 to-indigo-50 rounded-xl">
                  <div className="inline-flex items-center justify-center w-12 h-12 bg-blue-100 rounded-full mb-3">
                    <svg className="w-6 h-6 text-blue-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" />
                    </svg>
                  </div>
                  <p className="text-3xl font-bold text-gray-900">{formatNumber(userStats.newUsers?.today)}</p>
                  <p className="text-sm text-gray-500 mt-1">Today</p>
                </div>

                {/* This Week */}
                <div className="text-center p-4 bg-gradient-to-br from-green-50 to-emerald-50 rounded-xl">
                  <div className="inline-flex items-center justify-center w-12 h-12 bg-green-100 rounded-full mb-3">
                    <svg className="w-6 h-6 text-green-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z" />
                    </svg>
                  </div>
                  <p className="text-3xl font-bold text-gray-900">{formatNumber(userStats.newUsers?.thisWeek)}</p>
                  <p className="text-sm text-gray-500 mt-1">This Week</p>
                </div>

                {/* This Month */}
                <div className="text-center p-4 bg-gradient-to-br from-purple-50 to-pink-50 rounded-xl">
                  <div className="inline-flex items-center justify-center w-12 h-12 bg-purple-100 rounded-full mb-3">
                    <svg className="w-6 h-6 text-purple-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 19v-6a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2a2 2 0 002-2zm0 0V9a2 2 0 012-2h2a2 2 0 012 2v10m-6 0a2 2 0 002 2h2a2 2 0 002-2m0 0V5a2 2 0 012-2h2a2 2 0 012 2v14a2 2 0 01-2 2h-2a2 2 0 01-2-2z" />
                    </svg>
                  </div>
                  <p className="text-3xl font-bold text-gray-900">{formatNumber(userStats.newUsers?.thisMonth)}</p>
                  <p className="text-sm text-gray-500 mt-1">This Month</p>
                </div>

                {/* This Year */}
                <div className="text-center p-4 bg-gradient-to-br from-orange-50 to-amber-50 rounded-xl">
                  <div className="inline-flex items-center justify-center w-12 h-12 bg-orange-100 rounded-full mb-3">
                    <svg className="w-6 h-6 text-orange-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13 7h8m0 0v8m0-8l-8 8-4-4-6 6" />
                    </svg>
                  </div>
                  <p className="text-3xl font-bold text-gray-900">{formatNumber(userStats.newUsers?.thisYear)}</p>
                  <p className="text-sm text-gray-500 mt-1">This Year</p>
                </div>
              </div>
            </div>
          </>
        )}

        {/* ==================== ORDER STATISTICS SECTION ==================== */}
        {orderStatsError && (
          <div className="mt-6 bg-red-50 border border-red-200 rounded-lg p-4">
            <div className="flex items-center">
              <svg className="w-5 h-5 text-red-400 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
              </svg>
              <span className="text-red-700">Order Stats: {orderStatsError}</span>
            </div>
          </div>
        )}

        {orderStatsLoading && !orderStats && (
          <div className="mt-8 flex items-center justify-center py-12">
            <div className="text-center">
              <svg className="animate-spin h-10 w-10 text-purple-600 mx-auto mb-4" fill="none" viewBox="0 0 24 24">
                <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"></circle>
                <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
              </svg>
              <p className="text-gray-500">Loading order statistics...</p>
            </div>
          </div>
        )}

        {orderStats && (
          <>
            {/* Order Stats Header */}
            <div className="mt-12 mb-6">
              <h2 className="text-2xl font-bold text-gray-900">Order Statistics</h2>
              <p className="text-sm text-gray-500">Order analytics and revenue metrics</p>
            </div>

            {/* Revenue KPI Cards */}
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6 mb-8">
              {/* Total Orders */}
              <div className="bg-white rounded-xl shadow-sm border border-gray-100 p-6 hover:shadow-md transition-shadow">
                <div className="flex items-center justify-between">
                  <div>
                    <p className="text-sm font-medium text-gray-500">Total Orders</p>
                    <p className="mt-2 text-3xl font-bold text-gray-900">{formatNumber(orderStats.totalOrders)}</p>
                  </div>
                  <div className="p-3 bg-indigo-100 rounded-full">
                    <svg className="w-8 h-8 text-indigo-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2" />
                    </svg>
                  </div>
                </div>
              </div>

              {/* Total Revenue */}
              <div className="bg-white rounded-xl shadow-sm border border-gray-100 p-6 hover:shadow-md transition-shadow">
                <div className="flex items-center justify-between">
                  <div>
                    <p className="text-sm font-medium text-gray-500">Total Revenue</p>
                    <p className="mt-2 text-3xl font-bold text-green-600">{formatCurrency(orderStats.revenueStats?.totalRevenue)}</p>
                  </div>
                  <div className="p-3 bg-green-100 rounded-full">
                    <svg className="w-8 h-8 text-green-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 8c-1.657 0-3 .895-3 2s1.343 2 3 2 3 .895 3 2-1.343 2-3 2m0-8c1.11 0 2.08.402 2.599 1M12 8V7m0 1v8m0 0v1m0-1c-1.11 0-2.08-.402-2.599-1M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
                    </svg>
                  </div>
                </div>
              </div>

              {/* Today's Revenue */}
              <div className="bg-white rounded-xl shadow-sm border border-gray-100 p-6 hover:shadow-md transition-shadow">
                <div className="flex items-center justify-between">
                  <div>
                    <p className="text-sm font-medium text-gray-500">Today's Revenue</p>
                    <p className="mt-2 text-3xl font-bold text-blue-600">{formatCurrency(orderStats.revenueStats?.todayRevenue)}</p>
                  </div>
                  <div className="p-3 bg-blue-100 rounded-full">
                    <svg className="w-8 h-8 text-blue-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13 7h8m0 0v8m0-8l-8 8-4-4-6 6" />
                    </svg>
                  </div>
                </div>
              </div>

              {/* Orders Today */}
              <div className="bg-white rounded-xl shadow-sm border border-gray-100 p-6 hover:shadow-md transition-shadow">
                <div className="flex items-center justify-between">
                  <div>
                    <p className="text-sm font-medium text-gray-500">Orders Today</p>
                    <p className="mt-2 text-3xl font-bold text-purple-600">{formatNumber(orderStats.newOrdersStats?.today)}</p>
                  </div>
                  <div className="p-3 bg-purple-100 rounded-full">
                    <svg className="w-8 h-8 text-purple-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" />
                    </svg>
                  </div>
                </div>
              </div>
            </div>

            {/* Order Status Breakdown & Fulfillment */}
            <div className="grid grid-cols-1 lg:grid-cols-2 gap-6 mb-8">
              {/* Order Status Breakdown */}
              <div className="bg-white rounded-xl shadow-sm border border-gray-100 p-6">
                <h3 className="text-lg font-semibold text-gray-900 mb-4">Orders by Status</h3>
                <div className="space-y-3">
                  <div className="flex items-center justify-between p-3 bg-yellow-50 rounded-lg">
                    <span className="text-sm font-medium text-yellow-800">Pending</span>
                    <span className="text-lg font-bold text-yellow-600">{formatNumber(orderStats.ordersByStatus?.pending)}</span>
                  </div>
                  <div className="flex items-center justify-between p-3 bg-blue-50 rounded-lg">
                    <span className="text-sm font-medium text-blue-800">Placed</span>
                    <span className="text-lg font-bold text-blue-600">{formatNumber(orderStats.ordersByStatus?.placed)}</span>
                  </div>
                  <div className="flex items-center justify-between p-3 bg-indigo-50 rounded-lg">
                    <span className="text-sm font-medium text-indigo-800">Packed</span>
                    <span className="text-lg font-bold text-indigo-600">{formatNumber(orderStats.ordersByStatus?.packed)}</span>
                  </div>
                  <div className="flex items-center justify-between p-3 bg-purple-50 rounded-lg">
                    <span className="text-sm font-medium text-purple-800">Shipped</span>
                    <span className="text-lg font-bold text-purple-600">{formatNumber(orderStats.ordersByStatus?.shipped)}</span>
                  </div>
                  <div className="flex items-center justify-between p-3 bg-green-50 rounded-lg">
                    <span className="text-sm font-medium text-green-800">Delivered</span>
                    <span className="text-lg font-bold text-green-600">{formatNumber(orderStats.ordersByStatus?.delivered)}</span>
                  </div>
                  <div className="flex items-center justify-between p-3 bg-red-50 rounded-lg">
                    <span className="text-sm font-medium text-red-800">Cancelled</span>
                    <span className="text-lg font-bold text-red-600">{formatNumber(orderStats.ordersByStatus?.cancelled)}</span>
                  </div>
                </div>
              </div>

              {/* Fulfillment Stats */}
              <div className="bg-white rounded-xl shadow-sm border border-gray-100 p-6">
                <h3 className="text-lg font-semibold text-gray-900 mb-4">Fulfillment Overview</h3>
                <div className="grid grid-cols-2 gap-4">
                  <div className="text-center p-4 bg-gradient-to-br from-yellow-50 to-orange-50 rounded-xl">
                    <div className="inline-flex items-center justify-center w-10 h-10 bg-yellow-100 rounded-full mb-2">
                      <svg className="w-5 h-5 text-yellow-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" />
                      </svg>
                    </div>
                    <p className="text-2xl font-bold text-gray-900">{formatNumber(orderStats.fulfillmentStats?.pendingPayment)}</p>
                    <p className="text-xs text-gray-500">Pending Payment</p>
                  </div>
                  <div className="text-center p-4 bg-gradient-to-br from-blue-50 to-indigo-50 rounded-xl">
                    <div className="inline-flex items-center justify-center w-10 h-10 bg-blue-100 rounded-full mb-2">
                      <svg className="w-5 h-5 text-blue-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 8h14M5 8a2 2 0 110-4h14a2 2 0 110 4M5 8v10a2 2 0 002 2h10a2 2 0 002-2V8m-9 4h4" />
                      </svg>
                    </div>
                    <p className="text-2xl font-bold text-gray-900">{formatNumber(orderStats.fulfillmentStats?.toShip)}</p>
                    <p className="text-xs text-gray-500">To Ship</p>
                  </div>
                  <div className="text-center p-4 bg-gradient-to-br from-purple-50 to-pink-50 rounded-xl">
                    <div className="inline-flex items-center justify-center w-10 h-10 bg-purple-100 rounded-full mb-2">
                      <svg className="w-5 h-5 text-purple-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path d="M9 17a2 2 0 11-4 0 2 2 0 014 0zM19 17a2 2 0 11-4 0 2 2 0 014 0z" />
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13 16V6a1 1 0 00-1-1H4a1 1 0 00-1 1v10a1 1 0 001 1h1m8-1a1 1 0 01-1 1H9m4-1V8a1 1 0 011-1h2.586a1 1 0 01.707.293l3.414 3.414a1 1 0 01.293.707V16a1 1 0 01-1 1h-1m-6-1a1 1 0 001 1h1M5 17a2 2 0 104 0m-4 0a2 2 0 114 0m6 0a2 2 0 104 0m-4 0a2 2 0 114 0" />
                      </svg>
                    </div>
                    <p className="text-2xl font-bold text-gray-900">{formatNumber(orderStats.fulfillmentStats?.inTransit)}</p>
                    <p className="text-xs text-gray-500">In Transit</p>
                  </div>
                  <div className="text-center p-4 bg-gradient-to-br from-green-50 to-emerald-50 rounded-xl">
                    <div className="inline-flex items-center justify-center w-10 h-10 bg-green-100 rounded-full mb-2">
                      <svg className="w-5 h-5 text-green-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 13l4 4L19 7" />
                      </svg>
                    </div>
                    <p className="text-2xl font-bold text-gray-900">{formatNumber(orderStats.fulfillmentStats?.delivered)}</p>
                    <p className="text-xs text-gray-500">Delivered</p>
                  </div>
                </div>
              </div>
            </div>

            {/* Revenue Timeline */}
            <div className="bg-white rounded-xl shadow-sm border border-gray-100 p-6 mb-8">
              <h3 className="text-lg font-semibold text-gray-900 mb-6">Revenue Timeline</h3>
              <div className="grid grid-cols-2 md:grid-cols-5 gap-4">
                <div className="text-center p-4 bg-gradient-to-br from-green-50 to-emerald-50 rounded-xl">
                  <p className="text-2xl font-bold text-green-600">{formatCurrency(orderStats.revenueStats?.todayRevenue)}</p>
                  <p className="text-sm text-gray-500 mt-1">Today</p>
                </div>
                <div className="text-center p-4 bg-gradient-to-br from-blue-50 to-indigo-50 rounded-xl">
                  <p className="text-2xl font-bold text-blue-600">{formatCurrency(orderStats.revenueStats?.weekRevenue)}</p>
                  <p className="text-sm text-gray-500 mt-1">This Week</p>
                </div>
                <div className="text-center p-4 bg-gradient-to-br from-purple-50 to-pink-50 rounded-xl">
                  <p className="text-2xl font-bold text-purple-600">{formatCurrency(orderStats.revenueStats?.monthRevenue)}</p>
                  <p className="text-sm text-gray-500 mt-1">This Month</p>
                </div>
                <div className="text-center p-4 bg-gradient-to-br from-orange-50 to-amber-50 rounded-xl">
                  <p className="text-2xl font-bold text-orange-600">{formatCurrency(orderStats.revenueStats?.yearRevenue)}</p>
                  <p className="text-sm text-gray-500 mt-1">This Year</p>
                </div>
                <div className="text-center p-4 bg-gradient-to-br from-gray-50 to-slate-100 rounded-xl">
                  <p className="text-2xl font-bold text-gray-800">{formatCurrency(orderStats.revenueStats?.totalRevenue)}</p>
                  <p className="text-sm text-gray-500 mt-1">All Time</p>
                </div>
              </div>
            </div>

            {/* New Orders Timeline */}
            <div className="bg-white rounded-xl shadow-sm border border-gray-100 p-6">
              <h3 className="text-lg font-semibold text-gray-900 mb-6">New Orders</h3>
              <div className="grid grid-cols-3 gap-6">
                <div className="text-center p-4 bg-gradient-to-br from-blue-50 to-indigo-50 rounded-xl">
                  <div className="inline-flex items-center justify-center w-12 h-12 bg-blue-100 rounded-full mb-3">
                    <svg className="w-6 h-6 text-blue-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" />
                    </svg>
                  </div>
                  <p className="text-3xl font-bold text-gray-900">{formatNumber(orderStats.newOrdersStats?.today)}</p>
                  <p className="text-sm text-gray-500 mt-1">Today</p>
                </div>
                <div className="text-center p-4 bg-gradient-to-br from-green-50 to-emerald-50 rounded-xl">
                  <div className="inline-flex items-center justify-center w-12 h-12 bg-green-100 rounded-full mb-3">
                    <svg className="w-6 h-6 text-green-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z" />
                    </svg>
                  </div>
                  <p className="text-3xl font-bold text-gray-900">{formatNumber(orderStats.newOrdersStats?.thisWeek)}</p>
                  <p className="text-sm text-gray-500 mt-1">This Week</p>
                </div>
                <div className="text-center p-4 bg-gradient-to-br from-purple-50 to-pink-50 rounded-xl">
                  <div className="inline-flex items-center justify-center w-12 h-12 bg-purple-100 rounded-full mb-3">
                    <svg className="w-6 h-6 text-purple-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 19v-6a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2a2 2 0 002-2zm0 0V9a2 2 0 012-2h2a2 2 0 012 2v10m-6 0a2 2 0 002 2h2a2 2 0 002-2m0 0V5a2 2 0 012-2h2a2 2 0 012 2v14a2 2 0 01-2 2h-2a2 2 0 01-2-2z" />
                    </svg>
                  </div>
                  <p className="text-3xl font-bold text-gray-900">{formatNumber(orderStats.newOrdersStats?.thisMonth)}</p>
                  <p className="text-sm text-gray-500 mt-1">This Month</p>
                </div>
              </div>
            </div>
          </>
        )}
        </div>
      </div>
    </AdminLayout>
  );
};

export default AdminDashboard;
