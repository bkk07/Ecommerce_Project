import React, { useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useSelector } from 'react-redux';
import { selectIsAuthenticated, selectUser } from '../features/auth/authSlice';
import { getUserOrders } from '../api/orderApi';
import { 
  createRating, 
  updateRating, 
  deleteRating, 
  getUserRatingsForOrder 
} from '../api/ratingApi';

// Helper to parse shipping address JSON
const parseShippingAddress = (addressString) => {
  if (!addressString) return null;
  try {
    return JSON.parse(addressString);
  } catch {
    return { address: addressString };
  }
};

// Order status badge component
const StatusBadge = ({ status }) => {
  const statusStyles = {
    PENDING: 'bg-yellow-100 text-yellow-800',
    CONFIRMED: 'bg-blue-100 text-blue-800',
    PROCESSING: 'bg-indigo-100 text-indigo-800',
    SHIPPED: 'bg-purple-100 text-purple-800',
    DELIVERED: 'bg-green-100 text-green-800',
    CANCELLED: 'bg-red-100 text-red-800',
    FAILED: 'bg-red-100 text-red-800',
    REFUNDED: 'bg-gray-100 text-gray-800',
  };

  return (
    <span className={`inline-flex items-center px-3 py-1 rounded-full text-xs font-medium ${statusStyles[status] || 'bg-gray-100 text-gray-800'}`}>
      {status}
    </span>
  );
};

// Star Rating Component
const StarRating = ({ rating, onRatingChange, readonly = false, size = 'md' }) => {
  const [hoverRating, setHoverRating] = useState(0);
  const sizes = {
    sm: 'w-4 h-4',
    md: 'w-6 h-6',
    lg: 'w-8 h-8'
  };

  return (
    <div className="flex items-center gap-1">
      {[1, 2, 3, 4, 5].map((star) => (
        <button
          key={star}
          type="button"
          disabled={readonly}
          onClick={() => !readonly && onRatingChange && onRatingChange(star)}
          onMouseEnter={() => !readonly && setHoverRating(star)}
          onMouseLeave={() => !readonly && setHoverRating(0)}
          className={`${readonly ? 'cursor-default' : 'cursor-pointer'} transition-colors`}
        >
          <svg
            className={`${sizes[size]} ${
              (hoverRating || rating) >= star
                ? 'text-yellow-400 fill-yellow-400'
                : 'text-gray-300'
            }`}
            fill="none"
            stroke="currentColor"
            viewBox="0 0 24 24"
          >
            <path
              strokeLinecap="round"
              strokeLinejoin="round"
              strokeWidth={2}
              d="M11.049 2.927c.3-.921 1.603-.921 1.902 0l1.519 4.674a1 1 0 00.95.69h4.915c.969 0 1.371 1.24.588 1.81l-3.976 2.888a1 1 0 00-.363 1.118l1.518 4.674c.3.922-.755 1.688-1.538 1.118l-3.976-2.888a1 1 0 00-1.176 0l-3.976 2.888c-.783.57-1.838-.197-1.538-1.118l1.518-4.674a1 1 0 00-.363-1.118l-3.976-2.888c-.784-.57-.38-1.81.588-1.81h4.914a1 1 0 00.951-.69l1.519-4.674z"
            />
          </svg>
        </button>
      ))}
    </div>
  );
};

// Rating Modal Component
const RatingModal = ({ item, orderId, existingRating, onClose, onSubmit }) => {
  const [rating, setRating] = useState(existingRating?.rating || 0);
  const [message, setMessage] = useState(existingRating?.message || '');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  const handleSubmit = async () => {
    if (rating === 0) {
      setError('Please select a rating');
      return;
    }

    setLoading(true);
    setError(null);

    try {
      if (existingRating) {
        await updateRating(existingRating.id, { rating, message });
      } else {
        await createRating({
          sku: item.skuCode,
          orderId: orderId,
          rating,
          message
        });
      }
      onSubmit();
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to submit rating');
    } finally {
      setLoading(false);
    }
  };

  const handleDelete = async () => {
    if (!existingRating) return;
    
    setLoading(true);
    try {
      await deleteRating(existingRating.id);
      onSubmit();
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to delete rating');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 p-4">
      <div className="bg-white rounded-xl max-w-md w-full p-6">
        <div className="flex justify-between items-start mb-4">
          <h3 className="text-lg font-semibold text-gray-900">
            {existingRating ? 'Update Your Review' : 'Rate This Product'}
          </h3>
          <button onClick={onClose} className="text-gray-400 hover:text-gray-600">
            <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
            </svg>
          </button>
        </div>

        {/* Product Info */}
        <div className="flex items-center gap-3 mb-6 p-3 bg-gray-50 rounded-lg">
          <div className="w-16 h-16 rounded-lg bg-white border overflow-hidden flex-shrink-0">
            {item.imageUrl ? (
              <img src={item.imageUrl} alt={item.productName} className="w-full h-full object-cover" />
            ) : (
              <div className="w-full h-full flex items-center justify-center bg-gray-100">
                <svg className="w-8 h-8 text-gray-300" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 16l4.586-4.586a2 2 0 012.828 0L16 16m-2-2l1.586-1.586a2 2 0 012.828 0L20 14m-6-6h.01M6 20h12a2 2 0 002-2V6a2 2 0 00-2-2H6a2 2 0 00-2 2v12a2 2 0 002 2z" />
                </svg>
              </div>
            )}
          </div>
          <div className="flex-1 min-w-0">
            <p className="font-medium text-gray-900 truncate">{item.productName}</p>
            <p className="text-sm text-gray-500">SKU: {item.skuCode}</p>
          </div>
        </div>

        {/* Star Rating */}
        <div className="mb-4">
          <label className="block text-sm font-medium text-gray-700 mb-2">Your Rating</label>
          <StarRating rating={rating} onRatingChange={setRating} size="lg" />
        </div>

        {/* Review Message */}
        <div className="mb-4">
          <label className="block text-sm font-medium text-gray-700 mb-2">Your Review (Optional)</label>
          <textarea
            value={message}
            onChange={(e) => setMessage(e.target.value)}
            rows={3}
            className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500"
            placeholder="Share your experience with this product..."
          />
        </div>

        {error && (
          <div className="mb-4 p-3 bg-red-50 text-red-600 text-sm rounded-lg">
            {error}
          </div>
        )}

        {/* Actions */}
        <div className="flex gap-3">
          {existingRating && (
            <button
              onClick={handleDelete}
              disabled={loading}
              className="px-4 py-2 text-red-600 hover:bg-red-50 rounded-lg transition-colors"
            >
              Delete
            </button>
          )}
          <div className="flex-1" />
          <button
            onClick={onClose}
            className="px-4 py-2 text-gray-600 hover:bg-gray-100 rounded-lg transition-colors"
          >
            Cancel
          </button>
          <button
            onClick={handleSubmit}
            disabled={loading || rating === 0}
            className="px-6 py-2 bg-indigo-600 text-white rounded-lg hover:bg-indigo-700 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
          >
            {loading ? 'Submitting...' : existingRating ? 'Update' : 'Submit'}
          </button>
        </div>
      </div>
    </div>
  );
};

// Single order card component
const OrderCard = ({ order }) => {
  const [expanded, setExpanded] = useState(false);
  const [ratings, setRatings] = useState({});
  const [ratingModalItem, setRatingModalItem] = useState(null);
  const [loadingRatings, setLoadingRatings] = useState(false);
  const shippingAddress = parseShippingAddress(order.shippingAddress);
  const isDelivered = order.status === 'DELIVERED';

  const formatDate = (dateString) => {
    if (!dateString) return 'N/A';
    const date = new Date(dateString);
    return date.toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'long',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    });
  };

  // Fetch existing ratings when order is delivered and expanded
  useEffect(() => {
    if (isDelivered && expanded && Object.keys(ratings).length === 0) {
      fetchRatings();
    }
  }, [isDelivered, expanded]);

  const fetchRatings = async () => {
    setLoadingRatings(true);
    try {
      const orderRatings = await getUserRatingsForOrder(order.orderNumber);
      const ratingsMap = {};
      orderRatings.forEach(r => {
        ratingsMap[r.sku] = r;
      });
      setRatings(ratingsMap);
    } catch (err) {
      console.error('Failed to fetch ratings:', err);
    } finally {
      setLoadingRatings(false);
    }
  };

  const handleRatingSubmit = () => {
    setRatingModalItem(null);
    fetchRatings(); // Refresh ratings
  };

  return (
    <div className="bg-white rounded-xl shadow-sm border border-gray-200 overflow-hidden">
      {/* Order Header */}
      <div className="p-6 border-b border-gray-100">
        <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
          <div className="flex items-center space-x-4">
            <div className="w-12 h-12 bg-indigo-100 rounded-full flex items-center justify-center">
              <svg className="w-6 h-6 text-indigo-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M16 11V7a4 4 0 00-8 0v4M5 9h14l1 12H4L5 9z" />
              </svg>
            </div>
            <div>
              <h3 className="font-semibold text-gray-900">Order #{order.orderNumber}</h3>
              <p className="text-sm text-gray-500">{formatDate(order.orderDate)}</p>
            </div>
          </div>
          <div className="flex items-center space-x-4">
            <StatusBadge status={order.status} />
            <span className="text-lg font-bold text-indigo-600">
              ${Number(order.totalAmount).toFixed(2)}
            </span>
          </div>
        </div>
      </div>

      {/* Order Items Preview */}
      <div className="p-6">
        <div className="flex items-center justify-between mb-4">
          <span className="text-sm font-medium text-gray-700">
            {order.items?.length || 0} item{order.items?.length !== 1 ? 's' : ''}
          </span>
          <button
            onClick={() => setExpanded(!expanded)}
            className="text-sm text-indigo-600 hover:text-indigo-700 font-medium flex items-center"
          >
            {expanded ? 'Hide Details' : 'View Details'}
            <svg
              className={`w-4 h-4 ml-1 transform transition-transform ${expanded ? 'rotate-180' : ''}`}
              fill="none"
              stroke="currentColor"
              viewBox="0 0 24 24"
            >
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 9l-7 7-7-7" />
            </svg>
          </button>
        </div>

        {/* Items Preview (collapsed) */}
        {!expanded && order.items && order.items.length > 0 && (
          <div className="flex -space-x-2 overflow-hidden">
            {order.items.slice(0, 4).map((item, index) => (
              <div
                key={item.skuCode || index}
                className="w-12 h-12 rounded-lg border-2 border-white bg-gray-100 overflow-hidden flex-shrink-0"
              >
                {item.imageUrl ? (
                  <img src={item.imageUrl} alt={item.productName} className="w-full h-full object-cover" />
                ) : (
                  <div className="w-full h-full flex items-center justify-center">
                    <svg className="w-6 h-6 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 16l4.586-4.586a2 2 0 012.828 0L16 16m-2-2l1.586-1.586a2 2 0 012.828 0L20 14m-6-6h.01M6 20h12a2 2 0 002-2V6a2 2 0 00-2-2H6a2 2 0 00-2 2v12a2 2 0 002 2z" />
                    </svg>
                  </div>
                )}
              </div>
            ))}
            {order.items.length > 4 && (
              <div className="w-12 h-12 rounded-lg border-2 border-white bg-gray-200 flex items-center justify-center flex-shrink-0">
                <span className="text-xs font-medium text-gray-600">+{order.items.length - 4}</span>
              </div>
            )}
          </div>
        )}

        {/* Expanded Details */}
        {expanded && (
          <div className="space-y-4 mt-4">
            {/* Order Items */}
            <div className="space-y-3">
              {order.items?.map((item, index) => {
                const existingRating = ratings[item.skuCode];
                return (
                  <div
                    key={item.skuCode || index}
                    className="p-3 bg-gray-50 rounded-lg"
                  >
                    <div className="flex items-center space-x-4">
                      <div className="w-16 h-16 rounded-lg bg-white border border-gray-200 overflow-hidden flex-shrink-0">
                        {item.imageUrl ? (
                          <img src={item.imageUrl} alt={item.productName} className="w-full h-full object-cover" />
                        ) : (
                          <div className="w-full h-full flex items-center justify-center">
                            <svg className="w-8 h-8 text-gray-300" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 16l4.586-4.586a2 2 0 012.828 0L16 16m-2-2l1.586-1.586a2 2 0 012.828 0L20 14m-6-6h.01M6 20h12a2 2 0 002-2V6a2 2 0 00-2-2H6a2 2 0 00-2 2v12a2 2 0 002 2z" />
                            </svg>
                          </div>
                        )}
                      </div>
                      <div className="flex-1 min-w-0">
                        <h4 className="text-sm font-medium text-gray-900 truncate">{item.productName}</h4>
                        <p className="text-xs text-gray-500">SKU: {item.skuCode}</p>
                        <p className="text-xs text-gray-500">Qty: {item.quantity}</p>
                      </div>
                      <div className="text-right">
                        <p className="text-sm font-semibold text-gray-900">
                          ${(Number(item.price) * item.quantity).toFixed(2)}
                        </p>
                        <p className="text-xs text-gray-500">${Number(item.price).toFixed(2)} each</p>
                      </div>
                    </div>

                    {/* Rating Section - Only for DELIVERED orders */}
                    {isDelivered && (
                      <div className="mt-3 pt-3 border-t border-gray-200">
                        {loadingRatings ? (
                          <div className="text-xs text-gray-500">Loading rating...</div>
                        ) : existingRating ? (
                          <div className="flex items-center justify-between">
                            <div className="flex items-center gap-2">
                              <span className="text-xs text-gray-600">Your Rating:</span>
                              <StarRating rating={existingRating.rating} size="sm" readonly />
                              {existingRating.message && (
                                <span className="text-xs text-gray-500 truncate max-w-[150px]">
                                  "{existingRating.message}"
                                </span>
                              )}
                            </div>
                            <button
                              onClick={() => setRatingModalItem({ ...item, orderId: order.orderNumber })}
                              className="text-xs text-indigo-600 hover:text-indigo-700 font-medium flex items-center gap-1"
                            >
                              <svg className="w-3 h-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15.232 5.232l3.536 3.536m-2.036-5.036a2.5 2.5 0 113.536 3.536L6.5 21.036H3v-3.572L16.732 3.732z" />
                              </svg>
                              Edit
                            </button>
                          </div>
                        ) : (
                          <button
                            onClick={() => setRatingModalItem({ ...item, orderId: order.orderNumber })}
                            className="flex items-center gap-1 text-xs text-indigo-600 hover:text-indigo-700 font-medium"
                          >
                            <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M11.049 2.927c.3-.921 1.603-.921 1.902 0l1.519 4.674a1 1 0 00.95.69h4.915c.969 0 1.371 1.24.588 1.81l-3.976 2.888a1 1 0 00-.363 1.118l1.518 4.674c.3.922-.755 1.688-1.538 1.118l-3.976-2.888a1 1 0 00-1.176 0l-3.976 2.888c-.783.57-1.838-.197-1.538-1.118l1.518-4.674a1 1 0 00-.363-1.118l-3.976-2.888c-.784-.57-.38-1.81.588-1.81h4.914a1 1 0 00.951-.69l1.519-4.674z" />
                            </svg>
                            Rate this product
                          </button>
                        )}
                      </div>
                    )}
                  </div>
                );
              })}
            </div>

            {/* Shipping Address */}
            {shippingAddress && (
              <div className="mt-4 p-4 bg-gray-50 rounded-lg">
                <h4 className="text-sm font-medium text-gray-900 mb-2 flex items-center">
                  <svg className="w-4 h-4 mr-2 text-gray-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M17.657 16.657L13.414 20.9a1.998 1.998 0 01-2.827 0l-4.244-4.243a8 8 0 1111.314 0z" />
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 11a3 3 0 11-6 0 3 3 0 016 0z" />
                  </svg>
                  Shipping Address
                </h4>
                <div className="text-sm text-gray-600">
                  {shippingAddress.firstName && shippingAddress.lastName && (
                    <p className="font-medium">{shippingAddress.firstName} {shippingAddress.lastName}</p>
                  )}
                  {shippingAddress.address && <p>{shippingAddress.address}</p>}
                  {(shippingAddress.city || shippingAddress.state || shippingAddress.zipCode) && (
                    <p>
                      {shippingAddress.city}{shippingAddress.city && shippingAddress.state && ', '}
                      {shippingAddress.state} {shippingAddress.zipCode}
                    </p>
                  )}
                  {shippingAddress.phone && <p className="mt-1">Phone: {shippingAddress.phone}</p>}
                  {shippingAddress.email && <p>Email: {shippingAddress.email}</p>}
                </div>
              </div>
            )}
          </div>
        )}
      </div>

      {/* Rating Modal */}
      {ratingModalItem && (
        <RatingModal
          item={ratingModalItem}
          orderId={ratingModalItem.orderId}
          existingRating={ratings[ratingModalItem.skuCode]}
          onClose={() => setRatingModalItem(null)}
          onSubmit={handleRatingSubmit}
        />
      )}
    </div>
  );
};

const Orders = () => {
  const navigate = useNavigate();
  const isAuthenticated = useSelector(selectIsAuthenticated);
  const user = useSelector(selectUser);

  const [orders, setOrders] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    const fetchOrders = async () => {
      if (!isAuthenticated || !user?.userId) {
        setLoading(false);
        return;
      }

      try {
        setLoading(true);
        setError(null);
        const data = await getUserOrders(user.userId);
        setOrders(data || []);
      } catch (err) {
        console.error('Failed to fetch orders:', err);
        setError(err.response?.data?.message || 'Failed to load orders. Please try again.');
      } finally {
        setLoading(false);
      }
    };

    fetchOrders();
  }, [isAuthenticated, user?.userId]);

  // Not authenticated state
  if (!isAuthenticated) {
    return (
      <div className="min-h-[calc(100vh-200px)] flex items-center justify-center px-4">
        <div className="text-center max-w-md">
          <div className="mx-auto w-24 h-24 bg-gray-100 rounded-full flex items-center justify-center mb-6">
            <svg className="w-12 h-12 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z" />
            </svg>
          </div>
          <h2 className="text-2xl font-bold text-gray-900 mb-2">Please sign in</h2>
          <p className="text-gray-600 mb-6">Sign in to view your orders</p>
          <Link
            to="/login"
            className="inline-block px-8 py-3 bg-indigo-600 text-white font-semibold rounded-lg hover:bg-indigo-700 transition-colors"
          >
            Sign In
          </Link>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gray-50">
      {/* Breadcrumb */}
      <div className="bg-white border-b border-gray-200">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-4">
          <nav className="flex items-center space-x-2 text-sm">
            <Link to="/" className="text-gray-500 hover:text-indigo-600 transition-colors">
              Home
            </Link>
            <svg className="w-4 h-4 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 5l7 7-7 7" />
            </svg>
            <span className="text-gray-900 font-medium">My Orders</span>
          </nav>
        </div>
      </div>

      {/* Page Header */}
      <div className="bg-gradient-to-r from-indigo-600 to-purple-600 py-8">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <h1 className="text-3xl font-bold text-white">My Orders</h1>
          <p className="text-white/80 mt-1">View and track your order history</p>
        </div>
      </div>

      {/* Main Content */}
      <div className="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        {/* Loading State */}
        {loading && (
          <div className="flex flex-col items-center justify-center py-16">
            <svg className="animate-spin h-12 w-12 text-indigo-600 mb-4" fill="none" viewBox="0 0 24 24">
              <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"></circle>
              <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
            </svg>
            <p className="text-gray-600">Loading your orders...</p>
          </div>
        )}

        {/* Error State */}
        {!loading && error && (
          <div className="p-6 bg-red-50 border border-red-200 rounded-xl text-center">
            <svg className="w-12 h-12 text-red-400 mx-auto mb-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
            </svg>
            <h3 className="text-lg font-medium text-red-800 mb-2">Failed to load orders</h3>
            <p className="text-red-600 mb-4">{error}</p>
            <button
              onClick={() => window.location.reload()}
              className="px-6 py-2 bg-red-600 text-white rounded-lg hover:bg-red-700 transition-colors"
            >
              Try Again
            </button>
          </div>
        )}

        {/* Empty State */}
        {!loading && !error && orders.length === 0 && (
          <div className="text-center py-16">
            <div className="mx-auto w-24 h-24 bg-gray-100 rounded-full flex items-center justify-center mb-6">
              <svg className="w-12 h-12 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M16 11V7a4 4 0 00-8 0v4M5 9h14l1 12H4L5 9z" />
              </svg>
            </div>
            <h2 className="text-2xl font-bold text-gray-900 mb-2">No orders yet</h2>
            <p className="text-gray-600 mb-6">When you place orders, they will appear here</p>
            <Link
              to="/"
              className="inline-block px-8 py-3 bg-indigo-600 text-white font-semibold rounded-lg hover:bg-indigo-700 transition-colors"
            >
              Start Shopping
            </Link>
          </div>
        )}

        {/* Orders List */}
        {!loading && !error && orders.length > 0 && (
          <div className="space-y-6">
            {orders.map((order) => (
              <OrderCard key={order.orderNumber} order={order} />
            ))}
          </div>
        )}
      </div>
    </div>
  );
};

export default Orders;
