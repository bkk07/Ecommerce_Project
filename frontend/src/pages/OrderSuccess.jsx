import React, { useEffect } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { useDispatch } from 'react-redux';
import { clearCartItems, resetCart } from '../features/cart/cartSlice';

const OrderSuccess = () => {
  const location = useLocation();
  const navigate = useNavigate();
  const dispatch = useDispatch();
  
  const { 
    paymentId, 
    orderId, 
    razorpayOrderId,
    amount,
    currency,
    status,
    verified, 
    verificationError,
    isBuyNow,
    purchasedItems
  } = location.state || {};

  useEffect(() => {
    // Only clear cart for non-Buy Now orders (cart-based checkout)
    if (paymentId && orderId && !isBuyNow) {
      // Clear cart from backend
      dispatch(clearCartItems())
        .then(() => {
          console.log('Cart cleared from backend after successful order');
        })
        .catch((error) => {
          console.error('Failed to clear cart from backend:', error);
          // Still reset local state even if backend call fails
          dispatch(resetCart());
        });
    }
  }, [dispatch, paymentId, orderId, isBuyNow]);

  // Redirect if no payment info
  useEffect(() => {
    if (!paymentId && !orderId) {
      // Allow viewing the page for a moment before redirecting
      const timer = setTimeout(() => {
        navigate('/');
      }, 5000);
      return () => clearTimeout(timer);
    }
  }, [paymentId, orderId, navigate]);

  return (
    <div className="min-h-[calc(100vh-200px)] flex items-center justify-center px-4 py-12">
      <div className="max-w-md w-full text-center">
        {/* Success Animation */}
        <div className="mx-auto w-24 h-24 bg-green-100 rounded-full flex items-center justify-center mb-8 animate-bounce">
          <svg className="w-12 h-12 text-green-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 13l4 4L19 7" />
          </svg>
        </div>

        <h1 className="text-3xl font-bold text-gray-900 mb-4">Order Placed Successfully!</h1>
        <p className="text-gray-600 mb-8">
          Thank you for your purchase. Your order has been confirmed and will be shipped soon.
        </p>

        {/* Order Details */}
        {(paymentId || orderId) && (
          <div className="bg-gray-50 rounded-xl p-6 mb-8 text-left">
            <h2 className="text-lg font-semibold text-gray-900 mb-4">Order Details</h2>
            <div className="space-y-3">
              {orderId && (
                <div className="flex justify-between">
                  <span className="text-gray-600">Order ID:</span>
                  <span className="font-medium text-gray-900 text-sm break-all">{orderId}</span>
                </div>
              )}
              {razorpayOrderId && razorpayOrderId !== orderId && (
                <div className="flex justify-between">
                  <span className="text-gray-600">Transaction ID:</span>
                  <span className="font-medium text-gray-900 text-sm break-all">{razorpayOrderId}</span>
                </div>
              )}
              {paymentId && (
                <div className="flex justify-between">
                  <span className="text-gray-600">Payment ID:</span>
                  <span className="font-medium text-gray-900 text-sm break-all">{paymentId}</span>
                </div>
              )}
              {amount && (
                <div className="flex justify-between">
                  <span className="text-gray-600">Amount Paid:</span>
                  <span className="font-medium text-green-600 text-sm">
                    {currency || 'INR'} {Number(amount).toFixed(2)}
                  </span>
                </div>
              )}
              <div className="flex justify-between">
                <span className="text-gray-600">Payment Status:</span>
                {verified ? (
                  <span className="px-2 py-1 bg-green-100 text-green-700 text-xs font-medium rounded-full">
                    {status || 'Payment Verified'} ✓
                  </span>
                ) : verified === false ? (
                  <span className="px-2 py-1 bg-yellow-100 text-yellow-700 text-xs font-medium rounded-full">
                    Verification Pending
                  </span>
                ) : (
                  <span className="px-2 py-1 bg-green-100 text-green-700 text-xs font-medium rounded-full">
                    Payment Confirmed
                  </span>
                )}
              </div>
              {verificationError && (
                <div className="mt-2 p-2 bg-yellow-50 border border-yellow-200 rounded text-xs text-yellow-700">
                  Note: {verificationError}. Your payment was successful and will be confirmed shortly.
                </div>
              )}
            </div>
          </div>
        )}

        {/* What's Next */}
        <div className="bg-indigo-50 rounded-xl p-6 mb-8 text-left">
          <h3 className="text-md font-semibold text-indigo-900 mb-3">What's Next?</h3>
          <ul className="space-y-2 text-sm text-indigo-700">
            <li className="flex items-start">
              <svg className="w-5 h-5 text-indigo-500 mr-2 flex-shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M3 8l7.89 5.26a2 2 0 002.22 0L21 8M5 19h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v10a2 2 0 002 2z" />
              </svg>
              You'll receive an order confirmation email shortly
            </li>
            <li className="flex items-start">
              <svg className="w-5 h-5 text-indigo-500 mr-2 flex-shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2" />
              </svg>
              We'll notify you when your order ships
            </li>
            <li className="flex items-start">
              <svg className="w-5 h-5 text-indigo-500 mr-2 flex-shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M17.657 16.657L13.414 20.9a1.998 1.998 0 01-2.827 0l-4.244-4.243a8 8 0 1111.314 0z" />
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 11a3 3 0 11-6 0 3 3 0 016 0z" />
              </svg>
              Track your order from your account
            </li>
          </ul>
        </div>

        {/* Action Buttons */}
        <div className="flex flex-col sm:flex-row gap-4 justify-center">
          <Link
            to="/"
            className="px-8 py-3 bg-indigo-600 text-white font-semibold rounded-lg hover:bg-indigo-700 transition-colors"
          >
            Continue Shopping
          </Link>
          <Link
            to="/orders"
            className="px-8 py-3 border border-gray-300 text-gray-700 font-semibold rounded-lg hover:bg-gray-50 transition-colors"
          >
            View Orders
          </Link>
        </div>
      </div>
    </div>
  );
};

export default OrderSuccess;
