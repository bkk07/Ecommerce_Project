import React, { useCallback, useEffect, useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
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

// Order steps matching backend OrderStatus enum
const ORDER_STEPS = ['PENDING', 'CONFIRMED', 'PAYMENT_READY', 'PLACED', 'PACKED', 'SHIPPED', 'DELIVERED'];

// User-friendly display names for order steps
const STEP_LABELS = {
  PENDING: 'Pending',
  CONFIRMED: 'Confirmed',
  PAYMENT_READY: 'Payment Ready',
  PLACED: 'Order Placed',
  PACKED: 'Packed',
  SHIPPED: 'Shipped',
  DELIVERED: 'Delivered',
  CANCEL_REQUESTED: 'Cancellation Requested',
  CANCELLED: 'Cancelled'
};

const getStatusIndex = (status) => ORDER_STEPS.indexOf(status);
const getStepLabel = (step) => STEP_LABELS[step] || step;

// Order status badge component
const StatusBadge = ({ status }) => {
  const statusStyles = {
    PENDING: 'bg-amber-50 text-amber-700 ring-amber-200',
    CONFIRMED: 'bg-blue-50 text-blue-700 ring-blue-200',
    PAYMENT_READY: 'bg-cyan-50 text-cyan-700 ring-cyan-200',
    PLACED: 'bg-indigo-50 text-indigo-700 ring-indigo-200',
    PACKED: 'bg-purple-50 text-purple-700 ring-purple-200',
    SHIPPED: 'bg-violet-50 text-violet-700 ring-violet-200',
    DELIVERED: 'bg-emerald-50 text-emerald-700 ring-emerald-200',
    CANCEL_REQUESTED: 'bg-orange-50 text-orange-700 ring-orange-200',
    CANCELLED: 'bg-rose-50 text-rose-700 ring-rose-200',
  };

  return (
    <span
      className={`inline-flex items-center gap-2 rounded-full px-3 py-1 text-xs font-semibold ring-1 ${
        statusStyles[status] || 'bg-slate-100 text-slate-700 ring-slate-200'
      }`}
    >
      <span className="h-1.5 w-1.5 rounded-full bg-current opacity-80" />
      {getStepLabel(status)}
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
  const [ratingsLoaded, setRatingsLoaded] = useState(false);
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

  const fetchRatings = useCallback(async () => {
    if (!isDelivered || ratingsLoaded) return;
    setLoadingRatings(true);
    try {
      const orderRatings = await getUserRatingsForOrder(order.orderNumber);
      const ratingsMap = {};
      if (Array.isArray(orderRatings)) {
        orderRatings.forEach(r => {
          ratingsMap[r.sku] = r;
        });
      }
      setRatings(ratingsMap);
    } catch (err) {
      console.error('Failed to fetch ratings:', err);
      setRatings({});
    } finally {
      setLoadingRatings(false);
      setRatingsLoaded(true);
    }
  }, [isDelivered, order.orderNumber, ratingsLoaded]);

  // Fetch existing ratings when order is delivered and expanded
  useEffect(() => {
    if (isDelivered && expanded && !ratingsLoaded) {
      fetchRatings();
    }
  }, [isDelivered, expanded, ratingsLoaded, fetchRatings]);

  const handleRatingSubmit = () => {
    setRatingModalItem(null);
    setRatingsLoaded(false); // Reset so it refetches
    fetchRatings(); // Refresh ratings
  };

  const statusIndex = getStatusIndex(order.status);

  return (
    <div className="group overflow-hidden rounded-3xl border border-slate-200/80 bg-white shadow-lg shadow-slate-200/50 transition-all duration-300 hover:shadow-xl hover:border-slate-300">
      {/* Order Header */}
      <div className="relative overflow-hidden border-b border-slate-100 bg-gradient-to-r from-slate-50 via-white to-slate-50 p-6">
        <div className="absolute top-0 right-0 w-64 h-64 bg-gradient-to-br from-indigo-50 to-purple-50 rounded-full -mr-32 -mt-32 opacity-50" />
        <div className="relative flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
          <div className="flex items-center space-x-4">
            <div className="relative">
              <div className="flex h-14 w-14 items-center justify-center rounded-2xl bg-gradient-to-br from-indigo-500 to-purple-600 shadow-lg shadow-indigo-200">
                <svg className="h-7 w-7 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M20 7l-8-4-8 4m16 0l-8 4m8-4v10l-8 4m0-10L4 7m8 4v10M4 7v10l8 4" />
                </svg>
              </div>
              {isDelivered && (
                <div className="absolute -bottom-1 -right-1 w-5 h-5 bg-emerald-500 rounded-full flex items-center justify-center ring-2 ring-white">
                  <svg className="w-3 h-3 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={3} d="M5 13l4 4L19 7" />
                  </svg>
                </div>
              )}
            </div>
            <div>
              <Link to={`/orders/${order.orderNumber}`} className="text-lg font-bold text-slate-900 hover:text-indigo-600 transition-colors flex items-center gap-2">
                Order #{order.orderNumber}
                <svg className="w-4 h-4 opacity-0 -translate-x-2 group-hover:opacity-100 group-hover:translate-x-0 transition-all" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 5l7 7-7 7" />
                </svg>
              </Link>
              <p className="text-sm text-slate-500 flex items-center gap-2 mt-0.5">
                <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z" />
                </svg>
                {formatDate(order.orderDate)}
              </p>
            </div>
          </div>
          <div className="flex items-center gap-3 sm:gap-4">
            <Link 
              to={`/orders/${order.orderNumber}`}
              className="hidden sm:flex items-center gap-1 text-sm font-semibold text-indigo-600 hover:text-indigo-700 px-3 py-1.5 rounded-lg hover:bg-indigo-50 transition-all"
            >
              View Details
              <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 5l7 7-7 7" />
              </svg>
            </Link>
            <StatusBadge status={order.status} />
            <div className="text-right">
              <p className="text-2xl font-black bg-gradient-to-r from-indigo-600 to-purple-600 bg-clip-text text-transparent">
                ${Number(order.totalAmount).toFixed(2)}
              </p>
            </div>
          </div>
        </div>

        {/* Progress Timeline */}
        {!['CANCEL_REQUESTED', 'CANCELLED'].includes(order.status) && (
          <div className="mt-6 hidden sm:block">
            <div className="flex items-center gap-1">
              {ORDER_STEPS.map((step, index) => (
                <React.Fragment key={step}>
                  <div className="flex flex-col items-center gap-2 flex-shrink-0">
                    <div
                      className={`h-3 w-3 rounded-full transition-all duration-500 ${
                        statusIndex >= index 
                          ? 'bg-gradient-to-r from-indigo-500 to-purple-500 shadow-md shadow-indigo-200 scale-110' 
                          : 'bg-slate-200'
                      }`}
                    />
                    <span className={`text-[10px] font-semibold whitespace-nowrap ${
                      statusIndex >= index ? 'text-indigo-600' : 'text-slate-400'
                    }`}>
                      {getStepLabel(step)}
                    </span>
                  </div>
                  {index < ORDER_STEPS.length - 1 && (
                    <div className="h-1 flex-1 rounded-full overflow-hidden bg-slate-100 min-w-[20px]">
                      <div 
                        className={`h-full bg-gradient-to-r from-indigo-500 to-purple-500 transition-all duration-700 ${
                          statusIndex > index ? 'w-full' : 'w-0'
                        }`}
                      />
                    </div>
                  )}
                </React.Fragment>
              ))}
            </div>
          </div>
        )}
        
        {/* Cancelled Status Banner */}
        {['CANCEL_REQUESTED', 'CANCELLED'].includes(order.status) && (
          <div className={`mt-4 p-3 rounded-xl flex items-center gap-3 ${
            order.status === 'CANCEL_REQUESTED' 
              ? 'bg-orange-50 border border-orange-200' 
              : 'bg-rose-50 border border-rose-200'
          }`}>
            <div className={`p-2 rounded-lg ${
              order.status === 'CANCEL_REQUESTED' ? 'bg-orange-100' : 'bg-rose-100'
            }`}>
              <svg className={`w-5 h-5 ${
                order.status === 'CANCEL_REQUESTED' ? 'text-orange-600' : 'text-rose-600'
              }`} fill="none" stroke="currentColor" viewBox="0 0 24 24">
                {order.status === 'CANCEL_REQUESTED' ? (
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" />
                ) : (
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
                )}
              </svg>
            </div>
            <div>
              <p className={`text-sm font-semibold ${
                order.status === 'CANCEL_REQUESTED' ? 'text-orange-700' : 'text-rose-700'
              }`}>
                {order.status === 'CANCEL_REQUESTED' ? 'Cancellation in Progress' : 'Order Cancelled'}
              </p>
              <p className={`text-xs ${
                order.status === 'CANCEL_REQUESTED' ? 'text-orange-600' : 'text-rose-600'
              }`}>
                {order.status === 'CANCEL_REQUESTED' 
                  ? 'Your cancellation request is being processed' 
                  : 'This order has been cancelled'}
              </p>
            </div>
          </div>
        )}
      </div>

      {/* Order Items Preview */}
      <div className="p-6">
        <div className="mb-4 flex items-center justify-between">
          <div className="flex items-center gap-3">
            <div className="p-2 bg-slate-100 rounded-lg">
              <svg className="w-4 h-4 text-slate-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M20 7l-8-4-8 4m16 0l-8 4m8-4v10l-8 4m0-10L4 7m8 4v10M4 7v10l8 4" />
              </svg>
            </div>
            <span className="text-sm font-semibold text-slate-700">
              {order.items?.length || 0} item{order.items?.length !== 1 ? 's' : ''} in this order
            </span>
          </div>
          <button
            onClick={() => setExpanded(!expanded)}
            className="inline-flex items-center gap-2 rounded-xl px-4 py-2 text-sm font-semibold text-indigo-600 bg-indigo-50 transition-all duration-300 hover:bg-indigo-100 hover:shadow-md"
          >
            {expanded ? 'Collapse' : 'Expand'}
            <svg
              className={`h-4 w-4 transform transition-transform duration-300 ${expanded ? 'rotate-180' : ''}`}
              fill="none"
              stroke="currentColor"
              viewBox="0 0 24 24"
            >
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 9l-7 7-7-7" />
            </svg>
          </button>
        </div>

        {/* Items Preview (collapsed) - Stylish Grid */}
        {!expanded && order.items && order.items.length > 0 && (
          <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 gap-3">
            {order.items.slice(0, 4).map((item, index) => (
              <div
                key={item.skuCode || index}
                className="group relative overflow-hidden rounded-2xl border border-slate-200 bg-gradient-to-b from-white to-slate-50 p-3 transition-all duration-300 hover:border-indigo-200 hover:shadow-lg hover:-translate-y-1"
              >
                <div className="aspect-square w-full overflow-hidden rounded-xl bg-white mb-3 border border-slate-100">
                  {item.imageUrl ? (
                    <img src={item.imageUrl} alt={item.productName} className="h-full w-full object-cover transition-transform duration-300 group-hover:scale-110" />
                  ) : (
                    <div className="flex h-full w-full items-center justify-center bg-gradient-to-br from-slate-50 to-slate-100">
                      <svg className="h-12 w-12 text-slate-300" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M4 16l4.586-4.586a2 2 0 012.828 0L16 16m-2-2l1.586-1.586a2 2 0 012.828 0L20 14m-6-6h.01M6 20h12a2 2 0 002-2V6a2 2 0 00-2-2H6a2 2 0 00-2 2v12a2 2 0 002 2z" />
                      </svg>
                    </div>
                  )}
                </div>
                <p className="truncate text-sm font-semibold text-slate-800">{item.productName}</p>
                <div className="flex items-center justify-between mt-1">
                  <span className="text-xs text-slate-500">Qty: {item.quantity}</span>
                  <span className="text-xs font-semibold text-indigo-600">${Number(item.price).toFixed(2)}</span>
                </div>
              </div>
            ))}
            {order.items.length > 4 && (
              <div className="flex items-center justify-center rounded-2xl border-2 border-dashed border-slate-200 bg-slate-50 p-3">
                <div className="text-center">
                  <div className="w-12 h-12 mx-auto mb-2 rounded-full bg-indigo-100 flex items-center justify-center">
                    <span className="text-lg font-bold text-indigo-600">+{order.items.length - 4}</span>
                  </div>
                  <span className="text-xs font-medium text-slate-500">more items</span>
                </div>
              </div>
            )}
          </div>
        )}

        {/* Expanded Details */}
        {expanded && (
          <div className="mt-6 grid gap-6 lg:grid-cols-3">
            {/* Order Items */}
            <div className="space-y-4 lg:col-span-2">
              {order.items?.map((item, index) => {
                const existingRating = ratings[item.skuCode];
                return (
                  <div
                    key={item.skuCode || index}
                    className="group rounded-2xl border border-slate-200 bg-white p-4 shadow-sm transition-all duration-300 hover:shadow-lg hover:border-indigo-200"
                  >
                    <div className="flex items-center gap-4">
                      <div className="h-20 w-20 flex-shrink-0 overflow-hidden rounded-xl border border-slate-100 bg-white shadow-sm">
                        {item.imageUrl ? (
                          <img src={item.imageUrl} alt={item.productName} className="h-full w-full object-cover transition-transform duration-300 group-hover:scale-105" />
                        ) : (
                          <div className="flex h-full w-full items-center justify-center bg-gradient-to-br from-slate-50 to-slate-100">
                            <svg className="h-10 w-10 text-slate-300" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M4 16l4.586-4.586a2 2 0 012.828 0L16 16m-2-2l1.586-1.586a2 2 0 012.828 0L20 14m-6-6h.01M6 20h12a2 2 0 002-2V6a2 2 0 00-2-2H6a2 2 0 00-2 2v12a2 2 0 002 2z" />
                            </svg>
                          </div>
                        )}
                      </div>
                      <div className="min-w-0 flex-1">
                        <h4 className="text-base font-bold text-slate-900 group-hover:text-indigo-600 transition-colors">{item.productName}</h4>
                        <p className="text-xs text-slate-500 mt-1">SKU: {item.skuCode}</p>
                        <div className="flex items-center gap-4 mt-2">
                          <span className="inline-flex items-center gap-1 text-sm text-slate-600">
                            <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M7 7h.01M7 3h5c.512 0 1.024.195 1.414.586l7 7a2 2 0 010 2.828l-7 7a2 2 0 01-2.828 0l-7-7A1.994 1.994 0 013 12V7a4 4 0 014-4z" />
                            </svg>
                            Qty: <span className="font-semibold">{item.quantity}</span>
                          </span>
                        </div>
                      </div>
                      <div className="text-right">
                        <p className="text-lg font-black text-slate-900">
                          ${(Number(item.price) * item.quantity).toFixed(2)}
                        </p>
                        <p className="text-xs text-slate-500">${Number(item.price).toFixed(2)} each</p>
                      </div>
                    </div>

                    {/* Rating Section - Only for DELIVERED orders */}
                    <div className="mt-4 pt-4 border-t border-slate-100">
                      {isDelivered ? (
                        loadingRatings ? (
                          <div className="flex items-center gap-2 text-sm text-slate-500">
                            <div className="w-4 h-4 border-2 border-indigo-200 border-t-indigo-600 rounded-full animate-spin"></div>
                            Loading rating...
                          </div>
                        ) : existingRating ? (
                          <div className="flex items-center justify-between bg-gradient-to-r from-amber-50 to-yellow-50 rounded-xl p-3 border border-amber-100">
                            <div className="flex items-center gap-3">
                              <div className="p-2 bg-amber-100 rounded-lg">
                                <svg className="w-4 h-4 text-amber-600" fill="currentColor" viewBox="0 0 20 20">
                                  <path d="M9.049 2.927c.3-.921 1.603-.921 1.902 0l1.07 3.292a1 1 0 00.95.69h3.462c.969 0 1.371 1.24.588 1.81l-2.8 2.034a1 1 0 00-.364 1.118l1.07 3.292c.3.921-.755 1.688-1.54 1.118l-2.8-2.034a1 1 0 00-1.175 0l-2.8 2.034c-.784.57-1.838-.197-1.539-1.118l1.07-3.292a1 1 0 00-.364-1.118L2.98 8.72c-.783-.57-.38-1.81.588-1.81h3.461a1 1 0 00.951-.69l1.07-3.292z" />
                                </svg>
                              </div>
                              <div>
                                <div className="flex items-center gap-2">
                                  <span className="text-xs font-medium text-amber-700">Your Rating:</span>
                                  <StarRating rating={existingRating.rating} size="sm" readonly />
                                </div>
                                {existingRating.message && (
                                  <p className="max-w-[200px] truncate text-xs text-amber-600 mt-1">
                                    "{existingRating.message}"
                                  </p>
                                )}
                              </div>
                            </div>
                            <button
                              onClick={() => setRatingModalItem({ ...item, orderId: order.orderNumber })}
                              className="flex items-center gap-2 px-3 py-1.5 text-xs font-semibold text-amber-700 bg-white border border-amber-200 rounded-lg hover:bg-amber-50 transition-colors"
                            >
                              <svg className="h-3 w-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15.232 5.232l3.536 3.536m-2.036-5.036a2.5 2.5 0 113.536 3.536L6.5 21.036H3v-3.572L16.732 3.732z" />
                              </svg>
                              Edit
                            </button>
                          </div>
                        ) : (
                          <button
                            onClick={() => setRatingModalItem({ ...item, orderId: order.orderNumber })}
                            className="w-full flex items-center justify-center gap-2 py-2.5 text-sm font-semibold text-white bg-gradient-to-r from-amber-500 to-yellow-500 rounded-xl hover:from-amber-600 hover:to-yellow-600 transition-all duration-300 shadow-md hover:shadow-lg"
                          >
                            <svg className="h-4 w-4" fill="currentColor" viewBox="0 0 20 20">
                              <path d="M9.049 2.927c.3-.921 1.603-.921 1.902 0l1.07 3.292a1 1 0 00.95.69h3.462c.969 0 1.371 1.24.588 1.81l-2.8 2.034a1 1 0 00-.364 1.118l1.07 3.292c.3.921-.755 1.688-1.54 1.118l-2.8-2.034a1 1 0 00-1.175 0l-2.8 2.034c-.784.57-1.838-.197-1.539-1.118l1.07-3.292a1 1 0 00-.364-1.118L2.98 8.72c-.783-.57-.38-1.81.588-1.81h3.461a1 1 0 00.951-.69l1.07-3.292z" />
                            </svg>
                            Rate this product
                          </button>
                        )
                      ) : (
                        <div className="flex items-center gap-2 py-2 px-3 bg-slate-50 rounded-lg">
                          <svg className="w-4 h-4 text-slate-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" />
                          </svg>
                          <p className="text-xs text-slate-500">Rating available after delivery</p>
                        </div>
                      )}
                    </div>
                  </div>
                );
              })}
            </div>

            {/* Shipping Address */}
            {shippingAddress && (
              <div className="h-fit rounded-2xl border border-slate-200 bg-gradient-to-br from-white to-slate-50 p-5 shadow-sm">
                <div className="flex items-center gap-3 mb-4 pb-3 border-b border-slate-100">
                  <div className="p-2 bg-indigo-100 rounded-lg">
                    <svg className="h-5 w-5 text-indigo-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M17.657 16.657L13.414 20.9a1.998 1.998 0 01-2.827 0l-4.244-4.243a8 8 0 1111.314 0z" />
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 11a3 3 0 11-6 0 3 3 0 016 0z" />
                    </svg>
                  </div>
                  <h4 className="text-sm font-bold text-slate-900">Shipping Address</h4>
                </div>
                <div className="space-y-2 text-sm">
                  {shippingAddress.firstName && shippingAddress.lastName && (
                    <p className="font-semibold text-slate-800">{shippingAddress.firstName} {shippingAddress.lastName}</p>
                  )}
                  {shippingAddress.address && <p className="text-slate-600">{shippingAddress.address}</p>}
                  {(shippingAddress.city || shippingAddress.state || shippingAddress.zipCode) && (
                    <p className="text-slate-600">
                      {shippingAddress.city}{shippingAddress.city && shippingAddress.state && ', '}
                      {shippingAddress.state} {shippingAddress.zipCode}
                    </p>
                  )}
                  {(shippingAddress.phone || shippingAddress.email) && (
                    <div className="pt-3 mt-3 border-t border-slate-100 space-y-1">
                      {shippingAddress.phone && (
                        <p className="flex items-center gap-2 text-slate-600">
                          <svg className="w-4 h-4 text-slate-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M3 5a2 2 0 012-2h3.28a1 1 0 01.948.684l1.498 4.493a1 1 0 01-.502 1.21l-2.257 1.13a11.042 11.042 0 005.516 5.516l1.13-2.257a1 1 0 011.21-.502l4.493 1.498a1 1 0 01.684.949V19a2 2 0 01-2 2h-1C9.716 21 3 14.284 3 6V5z" />
                          </svg>
                          {shippingAddress.phone}
                        </p>
                      )}
                      {shippingAddress.email && (
                        <p className="flex items-center gap-2 text-slate-600">
                          <svg className="w-4 h-4 text-slate-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M3 8l7.89 5.26a2 2 0 002.22 0L21 8M5 19h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v10a2 2 0 002 2z" />
                          </svg>
                          {shippingAddress.email}
                        </p>
                      )}
                    </div>
                  )}
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

  const sortedOrders = useMemo(
    () =>
      [...orders].sort(
        (a, b) => new Date(b.orderDate || 0).getTime() - new Date(a.orderDate || 0).getTime()
      ),
    [orders]
  );

  const orderStats = useMemo(() => {
    const delivered = orders.filter((order) => order.status === 'DELIVERED').length;
    const inTransit = orders.filter((order) =>
      ['CONFIRMED', 'PROCESSING', 'SHIPPED'].includes(order.status)
    ).length;
    const totalSpent = orders.reduce((sum, order) => sum + Number(order.totalAmount || 0), 0);

    return {
      total: orders.length,
      delivered,
      inTransit,
      totalSpent,
    };
  }, [orders]);

  // Not authenticated state
  if (!isAuthenticated) {
    return (
      <div className="min-h-[calc(100vh-200px)] flex items-center justify-center px-4">
        <div className="max-w-md rounded-2xl border border-slate-200 bg-white p-8 text-center shadow-sm">
          <div className="mx-auto mb-6 flex h-24 w-24 items-center justify-center rounded-full bg-slate-100">
            <svg className="h-12 w-12 text-slate-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z" />
            </svg>
          </div>
          <h2 className="mb-2 text-2xl font-bold text-slate-900">Please sign in</h2>
          <p className="mb-6 text-slate-600">Sign in to view your orders</p>
          <Link
            to="/login"
            className="inline-block rounded-xl bg-indigo-600 px-8 py-3 font-semibold text-white transition hover:bg-indigo-700"
          >
            Sign In
          </Link>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gradient-to-b from-slate-50 via-white to-slate-50">
      {/* Page Header with elegant gradient */}
      <div className="relative overflow-hidden bg-gradient-to-br from-indigo-600 via-purple-600 to-violet-700">
        <div className="absolute inset-0 bg-white/5" style={{ backgroundImage: 'radial-gradient(circle at 1px 1px, rgba(255,255,255,0.15) 1px, transparent 0)', backgroundSize: '24px 24px' }} />
        <div className="relative max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-12">
          {/* Breadcrumb */}
          <nav className="flex items-center space-x-2 text-sm mb-6">
            <Link to="/" className="text-indigo-200 hover:text-white transition-colors">
              Home
            </Link>
            <svg className="w-4 h-4 text-indigo-300" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 5l7 7-7 7" />
            </svg>
            <span className="text-white font-medium">My Orders</span>
          </nav>
          
          <div className="flex items-center gap-4">
            <div className="p-4 bg-white/10 backdrop-blur-sm rounded-2xl">
              <svg className="w-10 h-10 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M20 7l-8-4-8 4m16 0l-8 4m8-4v10l-8 4m0-10L4 7m8 4v10M4 7v10l8 4" />
              </svg>
            </div>
            <div>
              <h1 className="text-3xl md:text-4xl font-bold text-white">My Orders</h1>
              <p className="mt-1 text-indigo-200">Track your orders and manage your purchases</p>
            </div>
          </div>
        </div>
        
        {/* Decorative elements */}
        <div className="absolute -bottom-1 left-0 right-0">
          <svg viewBox="0 0 1440 40" fill="none" xmlns="http://www.w3.org/2000/svg">
            <path d="M0 40V0C240 26.6667 480 40 720 40C960 40 1200 26.6667 1440 0V40H0Z" fill="rgb(248 250 252)" />
          </svg>
        </div>
      </div>

      {/* Main Content */}
      <div className="mx-auto max-w-7xl px-4 py-8 sm:px-6 lg:px-8 -mt-6">
        {/* Stats Cards */}
        {!loading && !error && orders.length > 0 && (
          <div className="mb-10 grid grid-cols-2 gap-4 lg:grid-cols-4">
            <div className="group relative overflow-hidden rounded-2xl bg-white p-6 shadow-lg shadow-slate-200/50 border border-slate-100 transition-all duration-300 hover:shadow-xl hover:-translate-y-1">
              <div className="absolute top-0 right-0 w-32 h-32 bg-gradient-to-br from-indigo-500/10 to-purple-500/10 rounded-full -mr-16 -mt-16 transition-transform duration-300 group-hover:scale-150" />
              <div className="relative">
                <div className="flex items-center justify-between mb-3">
                  <div className="p-2.5 bg-indigo-50 rounded-xl">
                    <svg className="w-6 h-6 text-indigo-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2" />
                    </svg>
                  </div>
                  <span className="text-xs font-medium text-indigo-600 bg-indigo-50 px-2 py-1 rounded-full">Total</span>
                </div>
                <p className="text-3xl font-black text-slate-900">{orderStats.total}</p>
                <p className="text-sm font-medium text-slate-500 mt-1">Total Orders</p>
              </div>
            </div>
            
            <div className="group relative overflow-hidden rounded-2xl bg-white p-6 shadow-lg shadow-slate-200/50 border border-slate-100 transition-all duration-300 hover:shadow-xl hover:-translate-y-1">
              <div className="absolute top-0 right-0 w-32 h-32 bg-gradient-to-br from-emerald-500/10 to-teal-500/10 rounded-full -mr-16 -mt-16 transition-transform duration-300 group-hover:scale-150" />
              <div className="relative">
                <div className="flex items-center justify-between mb-3">
                  <div className="p-2.5 bg-emerald-50 rounded-xl">
                    <svg className="w-6 h-6 text-emerald-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
                    </svg>
                  </div>
                  <span className="text-xs font-medium text-emerald-600 bg-emerald-50 px-2 py-1 rounded-full">Complete</span>
                </div>
                <p className="text-3xl font-black text-emerald-600">{orderStats.delivered}</p>
                <p className="text-sm font-medium text-slate-500 mt-1">Delivered</p>
              </div>
            </div>
            
            <div className="group relative overflow-hidden rounded-2xl bg-white p-6 shadow-lg shadow-slate-200/50 border border-slate-100 transition-all duration-300 hover:shadow-xl hover:-translate-y-1">
              <div className="absolute top-0 right-0 w-32 h-32 bg-gradient-to-br from-violet-500/10 to-purple-500/10 rounded-full -mr-16 -mt-16 transition-transform duration-300 group-hover:scale-150" />
              <div className="relative">
                <div className="flex items-center justify-between mb-3">
                  <div className="p-2.5 bg-violet-50 rounded-xl">
                    <svg className="w-6 h-6 text-violet-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13 16V6a1 1 0 00-1-1H4a1 1 0 00-1 1v10a1 1 0 001 1h1m8-1a1 1 0 01-1 1H9m4-1V8a1 1 0 011-1h2.586a1 1 0 01.707.293l3.414 3.414a1 1 0 01.293.707V16a1 1 0 01-1 1h-1m-6-1a1 1 0 001 1h1M5 17a2 2 0 104 0m-4 0a2 2 0 114 0m6 0a2 2 0 104 0m-4 0a2 2 0 114 0" />
                    </svg>
                  </div>
                  <span className="text-xs font-medium text-violet-600 bg-violet-50 px-2 py-1 rounded-full">Active</span>
                </div>
                <p className="text-3xl font-black text-violet-600">{orderStats.inTransit}</p>
                <p className="text-sm font-medium text-slate-500 mt-1">In Transit</p>
              </div>
            </div>
            
            <div className="group relative overflow-hidden rounded-2xl bg-gradient-to-br from-indigo-600 to-purple-600 p-6 shadow-lg shadow-indigo-200/50 transition-all duration-300 hover:shadow-xl hover:-translate-y-1">
              <div className="absolute top-0 right-0 w-32 h-32 bg-white/10 rounded-full -mr-16 -mt-16 transition-transform duration-300 group-hover:scale-150" />
              <div className="relative">
                <div className="flex items-center justify-between mb-3">
                  <div className="p-2.5 bg-white/20 rounded-xl backdrop-blur-sm">
                    <svg className="w-6 h-6 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 8c-1.657 0-3 .895-3 2s1.343 2 3 2 3 .895 3 2-1.343 2-3 2m0-8c1.11 0 2.08.402 2.599 1M12 8V7m0 1v8m0 0v1m0-1c-1.11 0-2.08-.402-2.599-1M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
                    </svg>
                  </div>
                  <span className="text-xs font-medium text-white/90 bg-white/20 px-2 py-1 rounded-full backdrop-blur-sm">Spent</span>
                </div>
                <p className="text-3xl font-black text-white">${orderStats.totalSpent.toFixed(2)}</p>
                <p className="text-sm font-medium text-indigo-200 mt-1">Total Spent</p>
              </div>
            </div>
          </div>
        )}

        {/* Loading State */}
        {loading && (
          <div className="flex flex-col items-center justify-center rounded-3xl bg-white py-20 shadow-lg shadow-slate-200/50 border border-slate-100">
            <div className="relative">
              <div className="w-20 h-20 rounded-full border-4 border-indigo-100 border-t-indigo-600 animate-spin" />
              <div className="absolute inset-0 flex items-center justify-center">
                <svg className="w-8 h-8 text-indigo-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M20 7l-8-4-8 4m16 0l-8 4m8-4v10l-8 4m0-10L4 7m8 4v10M4 7v10l8 4" />
                </svg>
              </div>
            </div>
            <p className="mt-6 text-lg font-medium text-slate-700">Loading your orders...</p>
            <p className="text-sm text-slate-500">Please wait a moment</p>
          </div>
        )}

        {/* Error State */}
        {!loading && error && (
          <div className="relative overflow-hidden rounded-3xl bg-gradient-to-br from-rose-50 to-orange-50 border border-rose-200 p-10 text-center">
            <div className="absolute top-0 right-0 w-48 h-48 bg-rose-200/30 rounded-full -mr-24 -mt-24" />
            <div className="relative">
              <div className="mx-auto w-20 h-20 bg-rose-100 rounded-full flex items-center justify-center mb-6">
                <svg className="w-10 h-10 text-rose-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
                </svg>
              </div>
              <h3 className="text-2xl font-bold text-rose-800 mb-2">Oops! Something went wrong</h3>
              <p className="text-rose-600 mb-6 max-w-md mx-auto">{error}</p>
              <button
                onClick={() => window.location.reload()}
                className="px-8 py-3 bg-gradient-to-r from-rose-500 to-orange-500 text-white font-semibold rounded-xl hover:from-rose-600 hover:to-orange-600 transition-all duration-300 shadow-lg shadow-rose-200 hover:shadow-xl hover:-translate-y-0.5"
              >
                Try Again
              </button>
            </div>
          </div>
        )}

        {/* Empty State */}
        {!loading && !error && orders.length === 0 && (
          <div className="relative overflow-hidden rounded-3xl bg-white py-20 text-center shadow-lg shadow-slate-200/50 border border-slate-100">
            <div className="absolute top-0 left-1/2 -translate-x-1/2 w-96 h-96 bg-gradient-to-br from-indigo-50 to-purple-50 rounded-full -mt-48 opacity-70" />
            <div className="relative">
              <div className="mx-auto w-32 h-32 bg-gradient-to-br from-indigo-100 to-purple-100 rounded-full flex items-center justify-center mb-8 shadow-lg shadow-indigo-100">
                <svg className="w-16 h-16 text-indigo-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M20 7l-8-4-8 4m16 0l-8 4m8-4v10l-8 4m0-10L4 7m8 4v10M4 7v10l8 4" />
                </svg>
              </div>
              <h2 className="text-3xl font-bold text-slate-900 mb-3">No orders yet</h2>
              <p className="text-slate-600 mb-8 max-w-md mx-auto">Start shopping to see your orders here. We have amazing products waiting for you!</p>
              <Link
                to="/"
                className="inline-flex items-center gap-2 px-8 py-4 bg-gradient-to-r from-indigo-600 to-purple-600 text-white font-semibold rounded-xl hover:from-indigo-700 hover:to-purple-700 transition-all duration-300 shadow-lg shadow-indigo-200 hover:shadow-xl hover:-translate-y-0.5"
              >
                <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M3 3h2l.4 2M7 13h10l4-8H5.4M7 13L5.4 5M7 13l-2.293 2.293c-.63.63-.184 1.707.707 1.707H17m0 0a2 2 0 100 4 2 2 0 000-4zm-8 2a2 2 0 11-4 0 2 2 0 014 0z" />
                </svg>
                Start Shopping
              </Link>
            </div>
          </div>
        )}

        {/* Orders List */}
        {!loading && !error && orders.length > 0 && (
          <div className="space-y-6">
            {sortedOrders.map((order, index) => (
              <div 
                key={order.orderNumber} 
                className="animate-slideUp"
                style={{ animationDelay: `${index * 50}ms` }}
              >
                <OrderCard order={order} />
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
};

export default Orders;
