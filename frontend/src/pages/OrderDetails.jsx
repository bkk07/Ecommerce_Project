import React, { useCallback, useEffect, useState } from 'react';
import { useParams, Link, useNavigate } from 'react-router-dom';
import { useSelector } from 'react-redux';
import { selectIsAuthenticated, selectUser } from '../features/auth/authSlice';
import { getOrderDetails } from '../api/orderApi';
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

  const statusIcons = {
    PENDING: (
      <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" />
      </svg>
    ),
    CONFIRMED: (
      <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
      </svg>
    ),
    PAYMENT_READY: (
      <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M3 10h18M7 15h1m4 0h1m-7 4h12a3 3 0 003-3V8a3 3 0 00-3-3H6a3 3 0 00-3 3v8a3 3 0 003 3z" />
      </svg>
    ),
    PLACED: (
      <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2m-6 9l2 2 4-4" />
      </svg>
    ),
    PACKED: (
      <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M20 7l-8-4-8 4m16 0l-8 4m8-4v10l-8 4m0-10L4 7m8 4v10M4 7v10l8 4" />
      </svg>
    ),
    SHIPPED: (
      <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13 16V6a1 1 0 00-1-1H4a1 1 0 00-1 1v10a1 1 0 001 1h1m8-1a1 1 0 01-1 1H9m4-1V8a1 1 0 011-1h2.586a1 1 0 01.707.293l3.414 3.414a1 1 0 01.293.707V16a1 1 0 01-1 1h-1m-6-1a1 1 0 001 1h1M5 17a2 2 0 104 0m-4 0a2 2 0 114 0m6 0a2 2 0 104 0m-4 0a2 2 0 114 0" />
      </svg>
    ),
    DELIVERED: (
      <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 13l4 4L19 7" />
      </svg>
    ),
    CANCEL_REQUESTED: (
      <svg className="w-4 h-4 animate-pulse" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
      </svg>
    ),
    CANCELLED: (
      <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
      </svg>
    ),
  };

  return (
    <span
      className={`inline-flex items-center gap-2 rounded-full px-4 py-1.5 text-sm font-semibold ring-1 transition-all duration-200 ${
        statusStyles[status] || 'bg-slate-100 text-slate-700 ring-slate-200'
      }`}
    >
      {statusIcons[status]}
      {getStepLabel(status)}
    </span>
  );
};

// Interactive Star Rating Component
const StarRating = ({ rating, onRatingChange, readonly = false, size = 'md', showValue = false }) => {
  const [hoverRating, setHoverRating] = useState(0);
  const sizes = {
    sm: 'w-4 h-4',
    md: 'w-6 h-6',
    lg: 'w-8 h-8',
    xl: 'w-10 h-10'
  };

  return (
    <div className="flex items-center gap-2">
      <div className="flex items-center gap-0.5">
        {[1, 2, 3, 4, 5].map((star) => (
          <button
            key={star}
            type="button"
            disabled={readonly}
            onClick={() => !readonly && onRatingChange && onRatingChange(star)}
            onMouseEnter={() => !readonly && setHoverRating(star)}
            onMouseLeave={() => !readonly && setHoverRating(0)}
            className={`${readonly ? 'cursor-default' : 'cursor-pointer transform hover:scale-110'} 
              transition-all duration-200 ease-out focus:outline-none`}
          >
            <svg
              className={`${sizes[size]} transition-colors duration-200 ${
                (hoverRating || rating) >= star
                  ? 'text-amber-400 fill-amber-400 drop-shadow-sm'
                  : 'text-slate-200 fill-slate-200'
              }`}
              viewBox="0 0 24 24"
            >
              <path d="M12 2l3.09 6.26L22 9.27l-5 4.87 1.18 6.88L12 17.77l-6.18 3.25L7 14.14 2 9.27l6.91-1.01L12 2z" />
            </svg>
          </button>
        ))}
      </div>
      {showValue && rating > 0 && (
        <span className="text-sm font-semibold text-slate-600">{rating}/5</span>
      )}
    </div>
  );
};

// Rating Card Component for Delivered Orders
const RatingCard = ({ item, orderId, existingRating, onRatingUpdate }) => {
  const [isRating, setIsRating] = useState(false);
  const [rating, setRating] = useState(existingRating?.rating || 0);
  const [message, setMessage] = useState(existingRating?.message || '');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [showSuccess, setShowSuccess] = useState(false);

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
      setShowSuccess(true);
      setTimeout(() => {
        setShowSuccess(false);
        setIsRating(false);
        onRatingUpdate();
      }, 1500);
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
      setRating(0);
      setMessage('');
      onRatingUpdate();
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to delete rating');
    } finally {
      setLoading(false);
    }
  };

  // Show existing rating display
  if (existingRating && !isRating) {
    return (
      <div className="rounded-2xl bg-gradient-to-br from-emerald-50 to-teal-50 p-5 border border-emerald-100 
        shadow-sm hover:shadow-md transition-all duration-300 animate-slideUp">
        <div className="flex items-start justify-between gap-4">
          <div className="flex-1">
            <div className="flex items-center gap-2 mb-2">
              <svg className="w-5 h-5 text-emerald-500" fill="currentColor" viewBox="0 0 20 20">
                <path fillRule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zm3.707-9.293a1 1 0 00-1.414-1.414L9 10.586 7.707 9.293a1 1 0 00-1.414 1.414l2 2a1 1 0 001.414 0l4-4z" clipRule="evenodd" />
              </svg>
              <span className="text-sm font-semibold text-emerald-700">Your Rating</span>
            </div>
            <div className="flex items-center gap-3 mb-3">
              <StarRating rating={existingRating.rating} readonly size="md" />
              <span className="text-lg font-bold text-slate-800">{existingRating.rating}/5</span>
            </div>
            {existingRating.message && (
              <p className="text-sm text-slate-600 italic bg-white/50 rounded-lg p-3 border border-emerald-100">
                "{existingRating.message}"
              </p>
            )}
          </div>
          <button
            onClick={() => {
              setIsRating(true);
              setRating(existingRating.rating);
              setMessage(existingRating.message || '');
            }}
            className="flex items-center gap-1.5 px-3 py-2 text-sm font-medium text-indigo-600 
              hover:text-indigo-700 hover:bg-indigo-50 rounded-lg transition-all duration-200"
          >
            <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} 
                d="M15.232 5.232l3.536 3.536m-2.036-5.036a2.5 2.5 0 113.536 3.536L6.5 21.036H3v-3.572L16.732 3.732z" />
            </svg>
            Edit rating
          </button>
        </div>
      </div>
    );
  }

  // Show rating input form
  if (isRating || !existingRating) {
    return (
      <div className={`rounded-2xl p-6 border shadow-sm hover:shadow-md transition-all duration-300 animate-slideUp
        ${existingRating 
          ? 'bg-white border-indigo-100' 
          : 'bg-gradient-to-br from-indigo-50 via-violet-50 to-purple-50 border-indigo-100'
        }`}>
        
        {showSuccess ? (
          <div className="flex flex-col items-center justify-center py-6 animate-scaleIn">
            <div className="w-16 h-16 rounded-full bg-emerald-100 flex items-center justify-center mb-4">
              <svg className="w-8 h-8 text-emerald-500 animate-checkmark" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2.5} d="M5 13l4 4L19 7" />
              </svg>
            </div>
            <p className="text-lg font-semibold text-emerald-700">Thank you for your feedback!</p>
          </div>
        ) : (
          <>
            <div className="flex items-start gap-4 mb-5">
              <div className="w-12 h-12 rounded-xl bg-gradient-to-br from-indigo-500 to-purple-600 
                flex items-center justify-center shadow-lg shadow-indigo-200 flex-shrink-0">
                <svg className="w-6 h-6 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} 
                    d="M11.049 2.927c.3-.921 1.603-.921 1.902 0l1.519 4.674a1 1 0 00.95.69h4.915c.969 0 1.371 1.24.588 1.81l-3.976 2.888a1 1 0 00-.363 1.118l1.518 4.674c.3.922-.755 1.688-1.538 1.118l-3.976-2.888a1 1 0 00-1.176 0l-3.976 2.888c-.783.57-1.838-.197-1.538-1.118l1.518-4.674a1 1 0 00-.363-1.118l-3.976-2.888c-.784-.57-.38-1.81.588-1.81h4.914a1 1 0 00.951-.69l1.519-4.674z" />
                </svg>
              </div>
              <div>
                <h4 className="text-lg font-bold text-slate-800">
                  {existingRating ? 'Update Your Review' : 'Rate This Product'}
                </h4>
                <p className="text-sm text-slate-500 mt-0.5">Your feedback helps other buyers</p>
              </div>
            </div>

            {/* Star Rating Input */}
            <div className="mb-5">
              <label className="block text-sm font-medium text-slate-700 mb-3">Tap to rate</label>
              <StarRating rating={rating} onRatingChange={setRating} size="xl" />
              {rating > 0 && (
                <p className="text-sm text-indigo-600 font-medium mt-2 animate-fadeIn">
                  {rating === 1 && "Poor"}
                  {rating === 2 && "Fair"}
                  {rating === 3 && "Good"}
                  {rating === 4 && "Very Good"}
                  {rating === 5 && "Excellent!"}
                </p>
              )}
            </div>

            {/* Review Message */}
            <div className="mb-5">
              <label className="block text-sm font-medium text-slate-700 mb-2">
                Share your experience <span className="text-slate-400 font-normal">(optional)</span>
              </label>
              <textarea
                value={message}
                onChange={(e) => setMessage(e.target.value)}
                rows={3}
                className="w-full px-4 py-3 border border-slate-200 rounded-xl focus:ring-2 
                  focus:ring-indigo-500/20 focus:border-indigo-400 transition-all duration-200
                  placeholder:text-slate-400 resize-none"
                placeholder="What did you like or dislike about this product?"
              />
            </div>

            {error && (
              <div className="mb-4 p-3 bg-red-50 text-red-600 text-sm rounded-xl border border-red-100 
                flex items-center gap-2 animate-shake">
                <svg className="w-5 h-5 flex-shrink-0" fill="currentColor" viewBox="0 0 20 20">
                  <path fillRule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zM8.707 7.293a1 1 0 00-1.414 1.414L8.586 10l-1.293 1.293a1 1 0 101.414 1.414L10 11.414l1.293 1.293a1 1 0 001.414-1.414L11.414 10l1.293-1.293a1 1 0 00-1.414-1.414L10 8.586 8.707 7.293z" clipRule="evenodd" />
                </svg>
                {error}
              </div>
            )}

            {/* Actions */}
            <div className="flex items-center gap-3">
              {existingRating && (
                <button
                  onClick={handleDelete}
                  disabled={loading}
                  className="px-4 py-2.5 text-sm font-medium text-red-600 hover:text-red-700 
                    hover:bg-red-50 rounded-xl transition-all duration-200"
                >
                  Delete
                </button>
              )}
              <div className="flex-1" />
              {existingRating && (
                <button
                  onClick={() => setIsRating(false)}
                  className="px-4 py-2.5 text-sm font-medium text-slate-600 hover:text-slate-700 
                    hover:bg-slate-100 rounded-xl transition-all duration-200"
                >
                  Cancel
                </button>
              )}
              <button
                onClick={handleSubmit}
                disabled={loading || rating === 0}
                className="px-6 py-2.5 bg-gradient-to-r from-indigo-600 to-purple-600 text-white 
                  rounded-xl font-semibold text-sm shadow-lg shadow-indigo-200 
                  hover:shadow-xl hover:shadow-indigo-300 hover:from-indigo-700 hover:to-purple-700
                  disabled:opacity-50 disabled:cursor-not-allowed disabled:shadow-none
                  transform hover:-translate-y-0.5 active:translate-y-0
                  transition-all duration-200"
              >
                {loading ? (
                  <span className="flex items-center gap-2">
                    <svg className="w-4 h-4 animate-spin" fill="none" viewBox="0 0 24 24">
                      <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
                      <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z" />
                    </svg>
                    Submitting...
                  </span>
                ) : existingRating ? 'Update Review' : 'Submit Review'}
              </button>
            </div>
          </>
        )}
      </div>
    );
  }

  return null;
};

// Product Item Card Component
const ProductItemCard = ({ item, orderId, isDelivered, rating, loadingRatings, onRatingUpdate }) => {
  return (
    <div className="group rounded-2xl border border-slate-100 bg-white p-5 shadow-sm 
      hover:shadow-lg hover:border-slate-200 transition-all duration-300">
      <div className="flex gap-4">
        {/* Product Image */}
        <Link 
          to={`/product/${item.skuCode}`}
          className="flex-shrink-0 w-24 h-24 md:w-28 md:h-28 rounded-xl overflow-hidden bg-slate-50 
            border border-slate-100 group-hover:border-indigo-200 transition-all duration-300"
        >
          {item.imageUrl ? (
            <img 
              src={item.imageUrl} 
              alt={item.productName} 
              className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-300" 
            />
          ) : (
            <div className="w-full h-full flex items-center justify-center">
              <svg className="w-10 h-10 text-slate-300" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} 
                  d="M4 16l4.586-4.586a2 2 0 012.828 0L16 16m-2-2l1.586-1.586a2 2 0 012.828 0L20 14m-6-6h.01M6 20h12a2 2 0 002-2V6a2 2 0 00-2-2H6a2 2 0 00-2 2v12a2 2 0 002 2z" />
              </svg>
            </div>
          )}
        </Link>

        {/* Product Details */}
        <div className="flex-1 min-w-0">
          <Link 
            to={`/product/${item.skuCode}`}
            className="text-base font-semibold text-slate-900 hover:text-indigo-600 
              transition-colors duration-200 line-clamp-2"
          >
            {item.productName}
          </Link>
          <p className="text-sm text-slate-500 mt-1">SKU: {item.skuCode}</p>
          <div className="flex items-center gap-4 mt-3">
            <span className="text-sm text-slate-600">
              Qty: <span className="font-medium text-slate-800">{item.quantity}</span>
            </span>
            <span className="text-sm text-slate-400">•</span>
            <span className="text-sm text-slate-600">
              ${Number(item.price).toFixed(2)} each
            </span>
          </div>
        </div>

        {/* Price */}
        <div className="text-right flex-shrink-0">
          <p className="text-lg font-bold text-slate-900">
            ${(Number(item.price) * item.quantity).toFixed(2)}
          </p>
        </div>
      </div>

      {/* Rating Section - Only for DELIVERED orders */}
      {isDelivered && (
        <div className="mt-5 pt-5 border-t border-slate-100">
          {loadingRatings && !rating ? (
            <div className="flex items-center gap-3 text-sm text-slate-500">
              <svg className="w-5 h-5 animate-spin text-indigo-500" fill="none" viewBox="0 0 24 24">
                <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
                <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z" />
              </svg>
              Loading rating...
            </div>
          ) : (
            <RatingCard 
              item={item} 
              orderId={orderId} 
              existingRating={rating}
              onRatingUpdate={onRatingUpdate}
            />
          )}
        </div>
      )}
    </div>
  );
};

// Order Timeline Component
const OrderTimeline = ({ status }) => {
  const statusIndex = getStatusIndex(status);
  const isCancelled = ['CANCEL_REQUESTED', 'CANCELLED'].includes(status);

  if (isCancelled) {
    return (
      <div className="flex items-center justify-center p-6 bg-rose-50 rounded-2xl border border-rose-100">
        <div className="flex items-center gap-3">
          <div className="w-10 h-10 rounded-full bg-rose-100 flex items-center justify-center">
            {status === 'CANCEL_REQUESTED' ? (
              <svg className="w-5 h-5 text-orange-600 animate-pulse" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
              </svg>
            ) : (
              <svg className="w-5 h-5 text-rose-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
              </svg>
            )}
          </div>
          <div>
            <p className={`font-semibold ${status === 'CANCEL_REQUESTED' ? 'text-orange-700' : 'text-rose-700'}`}>
              {status === 'CANCEL_REQUESTED' ? 'Cancellation Requested' : 'Cancelled'}
            </p>
            <p className={`text-sm ${status === 'CANCEL_REQUESTED' ? 'text-orange-600' : 'text-rose-600'}`}>
              {status === 'CANCEL_REQUESTED' 
                ? 'Your cancellation is being processed' 
                : 'This order has been cancelled'}
            </p>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="py-6">
      {/* Desktop Timeline */}
      <div className="hidden md:flex items-center justify-between">
        {ORDER_STEPS.map((step, index) => (
          <React.Fragment key={step}>
            <div className="flex flex-col items-center gap-3">
              <div 
                className={`w-12 h-12 rounded-full flex items-center justify-center transition-all duration-500 ${
                  statusIndex >= index 
                    ? 'bg-gradient-to-br from-indigo-500 to-purple-600 shadow-lg shadow-indigo-200' 
                    : 'bg-slate-100'
                }`}
              >
                {statusIndex > index ? (
                  <svg className="w-6 h-6 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2.5} d="M5 13l4 4L19 7" />
                  </svg>
                ) : statusIndex === index ? (
                  <div className="w-3 h-3 bg-white rounded-full animate-pulse" />
                ) : (
                  <div className="w-3 h-3 bg-slate-300 rounded-full" />
                )}
              </div>
              <span className={`text-sm font-semibold ${
                statusIndex >= index ? 'text-indigo-600' : 'text-slate-400'
              }`}>
                {getStepLabel(step)}
              </span>
            </div>
            {index < ORDER_STEPS.length - 1 && (
              <div className="flex-1 h-1 mx-2 rounded-full overflow-hidden bg-slate-100">
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

      {/* Mobile Timeline */}
      <div className="md:hidden space-y-4">
        {ORDER_STEPS.map((step, index) => (
          <div key={step} className="flex items-center gap-4">
            <div className="flex flex-col items-center">
              <div 
                className={`w-10 h-10 rounded-full flex items-center justify-center transition-all duration-500 ${
                  statusIndex >= index 
                    ? 'bg-gradient-to-br from-indigo-500 to-purple-600 shadow-lg shadow-indigo-200' 
                    : 'bg-slate-100'
                }`}
              >
                {statusIndex > index ? (
                  <svg className="w-5 h-5 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2.5} d="M5 13l4 4L19 7" />
                  </svg>
                ) : statusIndex === index ? (
                  <div className="w-2.5 h-2.5 bg-white rounded-full animate-pulse" />
                ) : (
                  <div className="w-2.5 h-2.5 bg-slate-300 rounded-full" />
                )}
              </div>
              {index < ORDER_STEPS.length - 1 && (
                <div className={`w-0.5 h-8 mt-2 rounded-full ${
                  statusIndex > index ? 'bg-indigo-500' : 'bg-slate-200'
                }`} />
              )}
            </div>
            <span className={`text-sm font-semibold ${
              statusIndex >= index ? 'text-indigo-600' : 'text-slate-400'
            }`}>
              {getStepLabel(step)}
            </span>
          </div>
        ))}
      </div>
    </div>
  );
};

// Main Order Details Component
const OrderDetails = () => {
  const { orderNumber } = useParams();
  const navigate = useNavigate();
  const isAuthenticated = useSelector(selectIsAuthenticated);
  const user = useSelector(selectUser);

  const [order, setOrder] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [ratings, setRatings] = useState({});
  const [loadingRatings, setLoadingRatings] = useState(true); // Start as true to prevent flicker
  const [ratingsLoaded, setRatingsLoaded] = useState(false);

  const isDelivered = order?.status === 'DELIVERED';
  const shippingAddress = order ? parseShippingAddress(order.shippingAddress) : null;

  const formatDate = (dateString) => {
    if (!dateString) return 'N/A';
    const date = new Date(dateString);
    return date.toLocaleDateString('en-US', {
      weekday: 'long',
      year: 'numeric',
      month: 'long',
      day: 'numeric',
    });
  };

  const formatTime = (dateString) => {
    if (!dateString) return '';
    const date = new Date(dateString);
    return date.toLocaleTimeString('en-US', {
      hour: '2-digit',
      minute: '2-digit',
    });
  };

  const fetchOrder = useCallback(async () => {
    if (!orderNumber) return;
    
    setLoading(true);
    setError(null);
    
    try {
      const data = await getOrderDetails(orderNumber);
      setOrder(data);
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to load order details');
    } finally {
      setLoading(false);
    }
  }, [orderNumber]);

  const fetchRatings = useCallback(async () => {
    if (!orderNumber) {
      setLoadingRatings(false);
      return;
    }
    
    setLoadingRatings(true);
    try {
      const orderRatings = await getUserRatingsForOrder(orderNumber);
      const ratingsMap = {};
      if (Array.isArray(orderRatings)) {
        orderRatings.forEach(r => {
          ratingsMap[r.sku] = r;
        });
      }
      setRatings(ratingsMap);
    } catch (err) {
      console.error('Failed to fetch ratings:', err);
      // Set empty ratings on error so the form shows
      setRatings({});
    } finally {
      setLoadingRatings(false);
      setRatingsLoaded(true);
    }
  }, [orderNumber]);

  useEffect(() => {
    if (!isAuthenticated) {
      navigate('/login', { state: { from: `/orders/${orderNumber}` } });
      return;
    }
    fetchOrder();
  }, [isAuthenticated, orderNumber, navigate, fetchOrder]);

  useEffect(() => {
    if (isDelivered && !ratingsLoaded) {
      fetchRatings();
    } else if (!isDelivered) {
      // Not delivered, no need to load ratings
      setLoadingRatings(false);
    }
  }, [isDelivered, ratingsLoaded, fetchRatings]);

  if (loading) {
    return (
      <div className="min-h-screen bg-gradient-to-b from-slate-50 to-white">
        <div className="max-w-4xl mx-auto px-4 py-12">
          <div className="animate-pulse space-y-6">
            <div className="h-8 bg-slate-200 rounded-lg w-48" />
            <div className="h-64 bg-slate-200 rounded-2xl" />
            <div className="h-48 bg-slate-200 rounded-2xl" />
          </div>
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="min-h-screen bg-gradient-to-b from-slate-50 to-white">
        <div className="max-w-4xl mx-auto px-4 py-12">
          <div className="bg-red-50 border border-red-100 rounded-2xl p-8 text-center">
            <div className="w-16 h-16 rounded-full bg-red-100 flex items-center justify-center mx-auto mb-4">
              <svg className="w-8 h-8 text-red-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
              </svg>
            </div>
            <h2 className="text-xl font-semibold text-red-700 mb-2">Failed to load order</h2>
            <p className="text-red-600 mb-6">{error}</p>
            <button
              onClick={fetchOrder}
              className="px-6 py-2.5 bg-red-600 text-white rounded-xl font-medium hover:bg-red-700 transition-colors"
            >
              Try Again
            </button>
          </div>
        </div>
      </div>
    );
  }

  if (!order) {
    return (
      <div className="min-h-screen bg-gradient-to-b from-slate-50 to-white">
        <div className="max-w-4xl mx-auto px-4 py-12 text-center">
          <p className="text-slate-600">Order not found</p>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gradient-to-b from-slate-50 to-white">
      <div className="max-w-4xl mx-auto px-4 py-8 md:py-12">
        {/* Back Button */}
        <Link 
          to="/orders"
          className="inline-flex items-center gap-2 text-slate-600 hover:text-indigo-600 
            font-medium mb-6 transition-colors duration-200"
        >
          <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M10 19l-7-7m0 0l7-7m-7 7h18" />
          </svg>
          Back to Orders
        </Link>

        {/* Order Header Card */}
        <div className="bg-white rounded-3xl shadow-sm border border-slate-100 overflow-hidden mb-6 animate-slideUp">
          <div className="bg-gradient-to-r from-indigo-600 via-purple-600 to-violet-600 p-6 md:p-8">
            <div className="flex flex-col md:flex-row md:items-center md:justify-between gap-4">
              <div>
                <p className="text-indigo-100 text-sm font-medium mb-1">Order Placed</p>
                <h1 className="text-2xl md:text-3xl font-bold text-white">#{orderNumber}</h1>
                <p className="text-indigo-100 mt-1">
                  {formatDate(order.orderDate)} at {formatTime(order.orderDate)}
                </p>
              </div>
              <div className="flex flex-col items-start md:items-end gap-3">
                <StatusBadge status={order.status} />
                <p className="text-3xl font-black text-white">
                  ${Number(order.totalAmount).toFixed(2)}
                </p>
              </div>
            </div>
          </div>

          {/* Order Timeline */}
          <div className="px-6 md:px-8">
            <OrderTimeline status={order.status} />
          </div>
        </div>

        {/* Main Content Grid */}
        <div className="grid gap-6 lg:grid-cols-3">
          {/* Order Items */}
          <div className="lg:col-span-2 space-y-4">
            <h2 className="text-lg font-bold text-slate-900 mb-4">
              Items ({order.items?.length || 0})
            </h2>
            {order.items?.map((item, index) => (
              <ProductItemCard 
                key={item.skuCode || index}
                item={item}
                orderId={orderNumber}
                isDelivered={isDelivered}
                rating={ratings[item.skuCode]}
                loadingRatings={loadingRatings}
                onRatingUpdate={fetchRatings}
              />
            ))}
          </div>

          {/* Sidebar */}
          <div className="space-y-6">
            {/* Shipping Address */}
            {shippingAddress && (
              <div className="bg-white rounded-2xl border border-slate-100 shadow-sm p-6 animate-slideUp">
                <div className="flex items-center gap-3 mb-4">
                  <div className="w-10 h-10 rounded-xl bg-indigo-50 flex items-center justify-center">
                    <svg className="w-5 h-5 text-indigo-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} 
                        d="M17.657 16.657L13.414 20.9a1.998 1.998 0 01-2.827 0l-4.244-4.243a8 8 0 1111.314 0z" />
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} 
                        d="M15 11a3 3 0 11-6 0 3 3 0 016 0z" />
                    </svg>
                  </div>
                  <h3 className="font-semibold text-slate-900">Shipping Address</h3>
                </div>
                <div className="text-sm text-slate-600 space-y-1">
                  {shippingAddress.firstName && shippingAddress.lastName && (
                    <p className="font-semibold text-slate-800">
                      {shippingAddress.firstName} {shippingAddress.lastName}
                    </p>
                  )}
                  {shippingAddress.address && <p>{shippingAddress.address}</p>}
                  {(shippingAddress.city || shippingAddress.state || shippingAddress.zipCode) && (
                    <p>
                      {shippingAddress.city}{shippingAddress.city && shippingAddress.state && ', '}
                      {shippingAddress.state} {shippingAddress.zipCode}
                    </p>
                  )}
                  {shippingAddress.phone && (
                    <p className="flex items-center gap-2 mt-3 pt-3 border-t border-slate-100">
                      <svg className="w-4 h-4 text-slate-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} 
                          d="M3 5a2 2 0 012-2h3.28a1 1 0 01.948.684l1.498 4.493a1 1 0 01-.502 1.21l-2.257 1.13a11.042 11.042 0 005.516 5.516l1.13-2.257a1 1 0 011.21-.502l4.493 1.498a1 1 0 01.684.949V19a2 2 0 01-2 2h-1C9.716 21 3 14.284 3 6V5z" />
                      </svg>
                      {shippingAddress.phone}
                    </p>
                  )}
                  {shippingAddress.email && (
                    <p className="flex items-center gap-2">
                      <svg className="w-4 h-4 text-slate-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} 
                          d="M3 8l7.89 5.26a2 2 0 002.22 0L21 8M5 19h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v10a2 2 0 002 2z" />
                      </svg>
                      {shippingAddress.email}
                    </p>
                  )}
                </div>
              </div>
            )}

            {/* Order Summary */}
            <div className="bg-white rounded-2xl border border-slate-100 shadow-sm p-6 animate-slideUp">
              <div className="flex items-center gap-3 mb-4">
                <div className="w-10 h-10 rounded-xl bg-emerald-50 flex items-center justify-center">
                  <svg className="w-5 h-5 text-emerald-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} 
                      d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2" />
                  </svg>
                </div>
                <h3 className="font-semibold text-slate-900">Order Summary</h3>
              </div>
              <div className="space-y-3">
                <div className="flex justify-between text-sm">
                  <span className="text-slate-600">Items ({order.items?.length || 0})</span>
                  <span className="font-medium text-slate-800">
                    ${order.items?.reduce((sum, item) => sum + (Number(item.price) * item.quantity), 0).toFixed(2)}
                  </span>
                </div>
                <div className="flex justify-between text-sm">
                  <span className="text-slate-600">Shipping</span>
                  <span className="font-medium text-emerald-600">Free</span>
                </div>
                <div className="border-t border-slate-100 pt-3 flex justify-between">
                  <span className="font-semibold text-slate-900">Total</span>
                  <span className="font-bold text-lg text-slate-900">
                    ${Number(order.totalAmount).toFixed(2)}
                  </span>
                </div>
              </div>
            </div>

            {/* Need Help Card */}
            <div className="bg-gradient-to-br from-slate-50 to-slate-100 rounded-2xl border border-slate-200 p-6">
              <div className="flex items-center gap-3 mb-3">
                <div className="w-10 h-10 rounded-xl bg-white flex items-center justify-center shadow-sm">
                  <svg className="w-5 h-5 text-slate-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} 
                      d="M8.228 9c.549-1.165 2.03-2 3.772-2 2.21 0 4 1.343 4 3 0 1.4-1.278 2.575-3.006 2.907-.542.104-.994.54-.994 1.093m0 3h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
                  </svg>
                </div>
                <h3 className="font-semibold text-slate-800">Need Help?</h3>
              </div>
              <p className="text-sm text-slate-600 mb-4">
                Have questions about your order? We're here to help.
              </p>
              <button className="w-full py-2.5 px-4 text-sm font-medium text-indigo-600 
                bg-white border border-indigo-200 rounded-xl hover:bg-indigo-50 
                hover:border-indigo-300 transition-all duration-200">
                Contact Support
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default OrderDetails;
