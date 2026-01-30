import React, { useEffect, useState } from 'react';
import { Link, useNavigate, useLocation } from 'react-router-dom';
import { useDispatch, useSelector } from 'react-redux';
import {
  fetchCart,
  updateCartItemPrice,
  clearCartItems,
  selectCartItems,
  selectCartTotal,
  selectCartLoading,
} from '../features/cart/cartSlice';
import { selectIsAuthenticated, selectUser } from '../features/auth/authSlice';
import { initiateCheckoutFromCart, initiateCheckoutWithItems, verifyPayment } from '../api/checkoutApi';

// Price update confirmation modal component
const PriceUpdateModal = ({ priceChanges, onConfirm, onCancel, isUpdating }) => {
  return (
    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 px-4">
      <div className="bg-white rounded-2xl shadow-2xl max-w-lg w-full p-6 animate-fade-in">
        <div className="flex items-center space-x-3 mb-4">
          <div className="w-12 h-12 bg-yellow-100 rounded-full flex items-center justify-center">
            <svg className="w-6 h-6 text-yellow-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
            </svg>
          </div>
          <div>
            <h3 className="text-xl font-bold text-gray-900">Price Changed</h3>
            <p className="text-sm text-gray-600">Some items in your cart have updated prices</p>
          </div>
        </div>

        <div className="space-y-3 max-h-60 overflow-y-auto mb-6">
          {priceChanges.map((item, index) => (
            <div key={index} className="flex items-center justify-between p-3 bg-gray-50 rounded-lg">
              <div className="flex-1">
                <p className="font-medium text-gray-900">{item.skuCode}</p>
                <div className="flex items-center space-x-2 text-sm">
                  <span className="text-gray-500 line-through">${Number(item.oldPrice).toFixed(2)}</span>
                  <svg className="w-4 h-4 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M14 5l7 7m0 0l-7 7m7-7H3" />
                  </svg>
                  <span className={`font-semibold ${item.currentPrice > item.oldPrice ? 'text-red-600' : 'text-green-600'}`}>
                    ${Number(item.currentPrice).toFixed(2)}
                  </span>
                </div>
              </div>
              <div className={`px-2 py-1 rounded text-xs font-medium ${item.currentPrice > item.oldPrice ? 'bg-red-100 text-red-700' : 'bg-green-100 text-green-700'}`}>
                {item.currentPrice > item.oldPrice ? '+' : '-'}${Math.abs(item.currentPrice - item.oldPrice).toFixed(2)}
              </div>
            </div>
          ))}
        </div>

        <p className="text-sm text-gray-600 mb-6">
          Would you like to continue with the updated prices?
        </p>

        <div className="flex space-x-3">
          <button
            onClick={onCancel}
            disabled={isUpdating}
            className="flex-1 py-3 px-4 border border-gray-300 text-gray-700 rounded-lg hover:bg-gray-50 transition-colors disabled:opacity-50"
          >
            Cancel
          </button>
          <button
            onClick={onConfirm}
            disabled={isUpdating}
            className="flex-1 py-3 px-4 bg-indigo-600 text-white rounded-lg hover:bg-indigo-700 transition-colors disabled:opacity-50 flex items-center justify-center"
          >
            {isUpdating ? (
              <>
                <svg className="animate-spin -ml-1 mr-2 h-4 w-4 text-white" fill="none" viewBox="0 0 24 24">
                  <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"></circle>
                  <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                </svg>
                Updating...
              </>
            ) : (
              'Update & Continue'
            )}
          </button>
        </div>
      </div>
    </div>
  );
};

// Error modal for validation failures
const ValidationErrorModal = ({ errors, onClose }) => {
  const getErrorMessage = (reason) => {
    switch (reason) {
      case 'SKU_NOT_FOUND':
        return 'Product not found';
      case 'PRODUCT_DISABLED':
        return 'Product is currently unavailable';
      case 'PRODUCT_DELETED':
        return 'Product has been removed';
      case 'VARIANT_DISABLED':
        return 'This variant is unavailable';
      case 'VARIANT_DELETED':
        return 'This variant has been removed';
      default:
        return 'Validation failed';
    }
  };

  return (
    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 px-4">
      <div className="bg-white rounded-2xl shadow-2xl max-w-lg w-full p-6">
        <div className="flex items-center space-x-3 mb-4">
          <div className="w-12 h-12 bg-red-100 rounded-full flex items-center justify-center">
            <svg className="w-6 h-6 text-red-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
            </svg>
          </div>
          <div>
            <h3 className="text-xl font-bold text-gray-900">Checkout Failed</h3>
            <p className="text-sm text-gray-600">Some items have issues</p>
          </div>
        </div>

        <div className="space-y-3 max-h-60 overflow-y-auto mb-6">
          {errors.map((item, index) => (
            <div key={index} className="flex items-center justify-between p-3 bg-red-50 rounded-lg">
              <div>
                <p className="font-medium text-gray-900">{item.skuCode}</p>
                <p className="text-sm text-red-600">{getErrorMessage(item.reason)}</p>
              </div>
              <svg className="w-5 h-5 text-red-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
              </svg>
            </div>
          ))}
        </div>

        <p className="text-sm text-gray-600 mb-6">
          Please remove the affected items from your cart and try again.
        </p>

        <button
          onClick={onClose}
          className="w-full py-3 px-4 bg-indigo-600 text-white rounded-lg hover:bg-indigo-700 transition-colors"
        >
          Go Back to Cart
        </button>
      </div>
    </div>
  );
};

const Checkout = () => {
  const dispatch = useDispatch();
  const navigate = useNavigate();
  const location = useLocation();
  
  // Check if this is a Buy Now checkout
  const buyNowItem = location.state?.buyNowItem;
  const isBuyNow = location.state?.isBuyNow || false;
  
  const isAuthenticated = useSelector(selectIsAuthenticated);
  const user = useSelector(selectUser);
  const cartItems = useSelector(selectCartItems);
  const cartTotal = useSelector(selectCartTotal);
  const isLoading = useSelector(selectCartLoading);

  // Determine which items to show - Buy Now item or cart items
  const items = isBuyNow && buyNowItem ? [buyNowItem] : cartItems;
  const totalAmount = isBuyNow && buyNowItem 
    ? buyNowItem.price * buyNowItem.quantity 
    : cartTotal;

  const [checkoutLoading, setCheckoutLoading] = useState(false);
  const [error, setError] = useState(null);
  const [showPriceModal, setShowPriceModal] = useState(false);
  const [showErrorModal, setShowErrorModal] = useState(false);
  const [priceChanges, setPriceChanges] = useState([]);
  const [validationErrors, setValidationErrors] = useState([]);
  const [isUpdatingPrices, setIsUpdatingPrices] = useState(false);
  const [formErrors, setFormErrors] = useState({});

  // Shipping form state
  const [shippingInfo, setShippingInfo] = useState({
    firstName: '',
    lastName: '',
    email: user?.email || '',
    address: '',
    city: '',
    state: '',
    zipCode: '',
    phone: '',
  });

  // Validate shipping form
  const validateShippingForm = () => {
    const errors = {};
    
    if (!shippingInfo.firstName.trim()) {
      errors.firstName = 'First name is required';
    }
    if (!shippingInfo.lastName.trim()) {
      errors.lastName = 'Last name is required';
    }
    if (!shippingInfo.email.trim()) {
      errors.email = 'Email is required';
    } else if (!/\S+@\S+\.\S+/.test(shippingInfo.email)) {
      errors.email = 'Please enter a valid email';
    }
    if (!shippingInfo.address.trim()) {
      errors.address = 'Street address is required';
    }
    if (!shippingInfo.city.trim()) {
      errors.city = 'City is required';
    }
    if (!shippingInfo.state.trim()) {
      errors.state = 'State is required';
    }
    if (!shippingInfo.zipCode.trim()) {
      errors.zipCode = 'ZIP code is required';
    }
    if (!shippingInfo.phone.trim()) {
      errors.phone = 'Phone number is required';
    }
    
    setFormErrors(errors);
    return Object.keys(errors).length === 0;
  };

  useEffect(() => {
    // Only fetch cart if not a Buy Now checkout
    if (isAuthenticated && !isBuyNow) {
      dispatch(fetchCart());
    }
  }, [dispatch, isAuthenticated, isBuyNow]);

  useEffect(() => {
    if (user?.email) {
      setShippingInfo(prev => ({ ...prev, email: user.email }));
    }
  }, [user]);

  const handleInputChange = (e) => {
    const { name, value } = e.target;
    setShippingInfo(prev => ({ ...prev, [name]: value }));
  };

  const handleCheckout = async () => {
    // Validate shipping form first
    if (!validateShippingForm()) {
      setError('Please fill in all required shipping information');
      return;
    }
    
    setCheckoutLoading(true);
    setError(null);

    // Convert shipping info to JSON string for backend
    const shippingAddressJson = JSON.stringify({
      firstName: shippingInfo.firstName,
      lastName: shippingInfo.lastName,
      email: shippingInfo.email,
      address: shippingInfo.address,
      city: shippingInfo.city,
      state: shippingInfo.state,
      zipCode: shippingInfo.zipCode,
      phone: shippingInfo.phone
    });

    try {
      let response;
      
      // Always use initiateCheckoutWithItems to ensure imageUrl is included
      // The backend's cart doesn't store imageUrl, so we send items from frontend
      const checkoutItems = items.map(item => ({
        skuCode: item.skuCode,
        quantity: item.quantity,
        price: item.price,
        productName: item.productName || item.name || '',
        imageUrl: item.imageUrl || ''
      }));
      
      response = await initiateCheckoutWithItems(checkoutItems, shippingAddressJson);
      
      if (response.status === 'SUCCESS') {
        // Checkout successful, open Razorpay
        openRazorpayPayment(response);
      } else if (response.status === 'FAILED') {
        // Check if it's a price mismatch
        const priceMismatchItems = response.itemErrors?.filter(
          item => item.reason === 'PRICE_MISMATCH'
        ) || [];
        
        const otherErrors = response.itemErrors?.filter(
          item => item.reason !== 'PRICE_MISMATCH'
        ) || [];

        if (priceMismatchItems.length > 0 && otherErrors.length === 0) {
          // Only price mismatches, show update confirmation
          const changes = priceMismatchItems.map(item => {
            const checkoutItem = items.find(i => i.skuCode === item.skuCode);
            return {
              skuCode: item.skuCode,
              oldPrice: checkoutItem?.price || 0,
              currentPrice: item.currentPrice,
            };
          });
          setPriceChanges(changes);
          setShowPriceModal(true);
        } else if (otherErrors.length > 0) {
          // Other validation errors
          setValidationErrors(otherErrors);
          setShowErrorModal(true);
        } else {
          setError(response.failureReason || 'Checkout failed');
        }
      }
    } catch (err) {
      setError(err.response?.data?.message || err.message || 'Checkout failed');
    } finally {
      setCheckoutLoading(false);
    }
  };

  const handlePriceUpdate = async () => {
    setIsUpdatingPrices(true);
    
    try {
      // Update prices in cart for each changed item
      for (const change of priceChanges) {
        await dispatch(updateCartItemPrice({ 
          skuCode: change.skuCode, 
          price: change.currentPrice 
        })).unwrap();
      }
      
      setShowPriceModal(false);
      setPriceChanges([]);
      
      // Retry checkout after price update
      handleCheckout();
    } catch (err) {
      setError('Failed to update prices. Please try again.');
      setShowPriceModal(false);
    } finally {
      setIsUpdatingPrices(false);
    }
  };

  const openRazorpayPayment = (checkoutResponse) => {
    const { razorpayOrderId, amount, currency } = checkoutResponse;

    // Load Razorpay script if not loaded
    if (!window.Razorpay) {
      const script = document.createElement('script');
      script.src = 'https://checkout.razorpay.com/v1/checkout.js';
      script.onload = () => initializeRazorpay(razorpayOrderId, amount, currency);
      document.body.appendChild(script);
    } else {
      initializeRazorpay(razorpayOrderId, amount, currency);
    }
  };

  const initializeRazorpay = (orderId, amount, currency) => {
    const options = {
      key: 'rzp_test_RyEfqhx8DTtt9q',
      amount: Math.round(amount * 100), // Razorpay expects amount in paise
      currency: currency || 'INR',
      name: 'E-Commerce Store',
      description: isBuyNow ? 'Buy Now - Direct Purchase' : 'Order Payment',
      order_id: orderId,
      handler: async function (response) {
        // Payment successful on Razorpay side, now verify with backend
        console.log('Razorpay payment response:', response);
        
        try {
          // Verify payment signature with backend
          const verifyResponse = await verifyPayment(
            response.razorpay_order_id,
            response.razorpay_payment_id,
            response.razorpay_signature
          );
          
          console.log('Payment verified successfully:', verifyResponse);
          
          // Only clear cart for cart-based checkout (not Buy Now)
          if (!isBuyNow) {
            try {
              await dispatch(clearCartItems()).unwrap();
              console.log('Cart cleared successfully after payment');
            } catch (clearError) {
              console.error('Failed to clear cart:', clearError);
              // Don't block the success flow if cart clear fails
            }
          }
          
          // Navigate to success page after verification
          navigate('/order-success', { 
            state: { 
              paymentId: response.razorpay_payment_id,
              orderId: verifyResponse.orderId || response.razorpay_order_id,
              razorpayOrderId: response.razorpay_order_id,
              amount: verifyResponse.amount,
              currency: verifyResponse.currency,
              status: verifyResponse.status,
              verified: verifyResponse.verified || true,
              isBuyNow: isBuyNow
            }
          });
        } catch (verifyError) {
          console.error('Payment verification failed:', verifyError);
          // Payment was successful but verification failed
          // Still navigate to success but mark as pending verification
          navigate('/order-success', { 
            state: { 
              paymentId: response.razorpay_payment_id,
              orderId: response.razorpay_order_id,
              razorpayOrderId: response.razorpay_order_id,
              verified: false,
              verificationError: verifyError.response?.data?.message || 'Verification pending'
            }
          });
        }
      },
      prefill: {
        name: `${shippingInfo.firstName} ${shippingInfo.lastName}`,
        email: shippingInfo.email,
        contact: shippingInfo.phone,
      },
      theme: {
        color: '#4F46E5',
      },
      modal: {
        ondismiss: function () {
          console.log('Payment cancelled by user');
        },
      },
    };

    const rzp = new window.Razorpay(options);
    rzp.on('payment.failed', function (response) {
      setError(`Payment failed: ${response.error.description}`);
    });
    rzp.open();
  };

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
          <p className="text-gray-600 mb-6">Sign in to continue with checkout</p>
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

  // Empty cart state (only for cart-based checkout, not Buy Now)
  if (!isLoading && items.length === 0 && !isBuyNow) {
    return (
      <div className="min-h-[calc(100vh-200px)] flex items-center justify-center px-4">
        <div className="text-center max-w-md">
          <div className="mx-auto w-24 h-24 bg-gray-100 rounded-full flex items-center justify-center mb-6">
            <svg className="w-12 h-12 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M3 3h2l.4 2M7 13h10l4-8H5.4M7 13L5.4 5M7 13l-2.293 2.293c-.63.63-.184 1.707.707 1.707H17m0 0a2 2 0 100 4 2 2 0 000-4zm-8 2a2 2 0 11-4 0 2 2 0 014 0z" />
            </svg>
          </div>
          <h2 className="text-2xl font-bold text-gray-900 mb-2">Your cart is empty</h2>
          <p className="text-gray-600 mb-6">Add some items to your cart to checkout</p>
          <Link
            to="/"
            className="inline-block px-8 py-3 bg-indigo-600 text-white font-semibold rounded-lg hover:bg-indigo-700 transition-colors"
          >
            Start Shopping
          </Link>
        </div>
      </div>
    );
  }

  const shipping = 9.99;
  const tax = totalAmount * 0.08;
  const total = totalAmount + shipping + tax;

  return (
    <div className="min-h-screen bg-gray-50">
      {/* Price Update Modal */}
      {showPriceModal && (
        <PriceUpdateModal
          priceChanges={priceChanges}
          onConfirm={handlePriceUpdate}
          onCancel={() => {
            setShowPriceModal(false);
            setPriceChanges([]);
          }}
          isUpdating={isUpdatingPrices}
        />
      )}

      {/* Validation Error Modal */}
      {showErrorModal && (
        <ValidationErrorModal
          errors={validationErrors}
          onClose={() => {
            setShowErrorModal(false);
            setValidationErrors([]);
            navigate('/cart');
          }}
        />
      )}

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
            {!isBuyNow && (
              <>
                <Link to="/cart" className="text-gray-500 hover:text-indigo-600 transition-colors">
                  Cart
                </Link>
                <svg className="w-4 h-4 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 5l7 7-7 7" />
                </svg>
              </>
            )}
            <span className="text-gray-900 font-medium">{isBuyNow ? 'Buy Now' : 'Checkout'}</span>
          </nav>
        </div>
      </div>

      {/* Page Header */}
      <div className="bg-gradient-to-r from-indigo-600 to-purple-600 py-8">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <h1 className="text-3xl font-bold text-white">{isBuyNow ? 'Buy Now - Quick Checkout' : 'Checkout'}</h1>
          <p className="text-white/80 mt-1">{isBuyNow ? 'Complete your direct purchase' : 'Complete your order'}</p>
        </div>
      </div>

      {/* Error Banner */}
      {error && (
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 mt-4">
          <div className="p-4 bg-red-50 border border-red-200 rounded-lg flex items-center">
            <svg className="w-5 h-5 text-red-500 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
            </svg>
            <span className="text-red-700">{error}</span>
            <button onClick={() => setError(null)} className="ml-auto text-red-500 hover:text-red-700">
              <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
              </svg>
            </button>
          </div>
        </div>
      )}

      {/* Main Content */}
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        <div className="flex flex-col lg:flex-row gap-8">
          {/* Left Column - Forms */}
          <div className="flex-1 space-y-6">
            {/* Shipping Information */}
            <div className="bg-white rounded-xl shadow-sm p-6">
              <h2 className="text-xl font-semibold text-gray-900 mb-6 flex items-center">
                <span className="w-8 h-8 bg-indigo-600 text-white rounded-full flex items-center justify-center text-sm font-bold mr-3">1</span>
                Shipping Information
                <span className="ml-2 text-sm font-normal text-red-500">* Required</span>
              </h2>
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-2">
                    First Name <span className="text-red-500">*</span>
                  </label>
                  <input
                    type="text"
                    name="firstName"
                    value={shippingInfo.firstName}
                    onChange={handleInputChange}
                    required
                    className={`w-full px-4 py-3 border rounded-lg focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-transparent ${
                      formErrors.firstName ? 'border-red-500 bg-red-50' : 'border-gray-300'
                    }`}
                    placeholder="John"
                  />
                  {formErrors.firstName && (
                    <p className="mt-1 text-sm text-red-500">{formErrors.firstName}</p>
                  )}
                </div>
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-2">
                    Last Name <span className="text-red-500">*</span>
                  </label>
                  <input
                    type="text"
                    name="lastName"
                    value={shippingInfo.lastName}
                    onChange={handleInputChange}
                    required
                    className={`w-full px-4 py-3 border rounded-lg focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-transparent ${
                      formErrors.lastName ? 'border-red-500 bg-red-50' : 'border-gray-300'
                    }`}
                    placeholder="Doe"
                  />
                  {formErrors.lastName && (
                    <p className="mt-1 text-sm text-red-500">{formErrors.lastName}</p>
                  )}
                </div>
                <div className="sm:col-span-2">
                  <label className="block text-sm font-medium text-gray-700 mb-2">
                    Email Address <span className="text-red-500">*</span>
                  </label>
                  <input
                    type="email"
                    name="email"
                    value={shippingInfo.email}
                    onChange={handleInputChange}
                    required
                    className={`w-full px-4 py-3 border rounded-lg focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-transparent ${
                      formErrors.email ? 'border-red-500 bg-red-50' : 'border-gray-300'
                    }`}
                    placeholder="john@example.com"
                  />
                  {formErrors.email && (
                    <p className="mt-1 text-sm text-red-500">{formErrors.email}</p>
                  )}
                </div>
                <div className="sm:col-span-2">
                  <label className="block text-sm font-medium text-gray-700 mb-2">
                    Street Address <span className="text-red-500">*</span>
                  </label>
                  <input
                    type="text"
                    name="address"
                    value={shippingInfo.address}
                    onChange={handleInputChange}
                    required
                    className={`w-full px-4 py-3 border rounded-lg focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-transparent ${
                      formErrors.address ? 'border-red-500 bg-red-50' : 'border-gray-300'
                    }`}
                    placeholder="123 Main Street"
                  />
                  {formErrors.address && (
                    <p className="mt-1 text-sm text-red-500">{formErrors.address}</p>
                  )}
                </div>
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-2">
                    City <span className="text-red-500">*</span>
                  </label>
                  <input
                    type="text"
                    name="city"
                    value={shippingInfo.city}
                    onChange={handleInputChange}
                    required
                    className={`w-full px-4 py-3 border rounded-lg focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-transparent ${
                      formErrors.city ? 'border-red-500 bg-red-50' : 'border-gray-300'
                    }`}
                    placeholder="New York"
                  />
                  {formErrors.city && (
                    <p className="mt-1 text-sm text-red-500">{formErrors.city}</p>
                  )}
                </div>
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-2">
                    State <span className="text-red-500">*</span>
                  </label>
                  <input
                    type="text"
                    name="state"
                    value={shippingInfo.state}
                    onChange={handleInputChange}
                    required
                    className={`w-full px-4 py-3 border rounded-lg focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-transparent ${
                      formErrors.state ? 'border-red-500 bg-red-50' : 'border-gray-300'
                    }`}
                    placeholder="NY"
                  />
                  {formErrors.state && (
                    <p className="mt-1 text-sm text-red-500">{formErrors.state}</p>
                  )}
                </div>
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-2">
                    ZIP Code <span className="text-red-500">*</span>
                  </label>
                  <input
                    type="text"
                    name="zipCode"
                    value={shippingInfo.zipCode}
                    onChange={handleInputChange}
                    required
                    className={`w-full px-4 py-3 border rounded-lg focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-transparent ${
                      formErrors.zipCode ? 'border-red-500 bg-red-50' : 'border-gray-300'
                    }`}
                    placeholder="10001"
                  />
                  {formErrors.zipCode && (
                    <p className="mt-1 text-sm text-red-500">{formErrors.zipCode}</p>
                  )}
                </div>
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-2">
                    Phone <span className="text-red-500">*</span>
                  </label>
                  <input
                    type="tel"
                    name="phone"
                    value={shippingInfo.phone}
                    onChange={handleInputChange}
                    required
                    className={`w-full px-4 py-3 border rounded-lg focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-transparent ${
                      formErrors.phone ? 'border-red-500 bg-red-50' : 'border-gray-300'
                    }`}
                    placeholder="(555) 123-4567"
                  />
                  {formErrors.phone && (
                    <p className="mt-1 text-sm text-red-500">{formErrors.phone}</p>
                  )}
                </div>
              </div>
            </div>

            {/* Payment Method Info */}
            <div className="bg-white rounded-xl shadow-sm p-6">
              <h2 className="text-xl font-semibold text-gray-900 mb-6 flex items-center">
                <span className="w-8 h-8 bg-indigo-600 text-white rounded-full flex items-center justify-center text-sm font-bold mr-3">2</span>
                Payment Method
              </h2>
              <div className="flex items-center p-4 bg-indigo-50 border-2 border-indigo-600 rounded-lg">
                <div className="w-12 h-12 bg-white rounded-lg flex items-center justify-center mr-4 shadow-sm">
                  <svg className="w-8 h-8 text-indigo-600" viewBox="0 0 24 24" fill="currentColor">
                    <path d="M20 4H4c-1.11 0-1.99.89-1.99 2L2 18c0 1.11.89 2 2 2h16c1.11 0 2-.89 2-2V6c0-1.11-.89-2-2-2zm0 14H4v-6h16v6zm0-10H4V6h16v2z" />
                  </svg>
                </div>
                <div>
                  <h3 className="font-semibold text-gray-900">Razorpay</h3>
                  <p className="text-sm text-gray-600">Pay securely with Credit/Debit Card, UPI, NetBanking</p>
                </div>
                <svg className="w-6 h-6 text-indigo-600 ml-auto" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 13l4 4L19 7" />
                </svg>
              </div>
            </div>
          </div>

          {/* Right Column - Order Summary */}
          <div className="lg:w-96">
            <div className="bg-white rounded-xl shadow-sm p-6 sticky top-24">
              <h2 className="text-xl font-semibold text-gray-900 mb-6">Order Summary</h2>

              {/* Cart Items */}
              <div className="space-y-4 mb-6 max-h-60 overflow-y-auto">
                {items.map((item) => (
                  <div key={item.skuCode} className="flex items-center space-x-4">
                    <div className="w-16 h-16 bg-gray-100 rounded-lg overflow-hidden flex-shrink-0">
                      {item.imageUrl ? (
                        <img
                          src={item.imageUrl}
                          alt={item.productName}
                          className="w-full h-full object-cover"
                        />
                      ) : (
                        <div className="w-full h-full flex items-center justify-center">
                          <svg className="w-8 h-8 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 16l4.586-4.586a2 2 0 012.828 0L16 16m-2-2l1.586-1.586a2 2 0 012.828 0L20 14m-6-6h.01M6 20h12a2 2 0 002-2V6a2 2 0 00-2-2H6a2 2 0 00-2 2v12a2 2 0 002 2z" />
                          </svg>
                        </div>
                      )}
                    </div>
                    <div className="flex-1 min-w-0">
                      <h3 className="text-sm font-medium text-gray-900 truncate">{item.productName || item.skuCode}</h3>
                      <p className="text-sm text-gray-500">Qty: {item.quantity}</p>
                    </div>
                    <span className="text-sm font-medium text-gray-900">
                      ${(Number(item.price) * item.quantity).toFixed(2)}
                    </span>
                  </div>
                ))}
              </div>

              {/* Divider */}
              <hr className="border-gray-200 mb-4" />

              {/* Price Breakdown */}
              <div className="space-y-3 mb-6">
                <div className="flex justify-between text-sm">
                  <span className="text-gray-600">Subtotal</span>
                  <span className="text-gray-900">${totalAmount.toFixed(2)}</span>
                </div>
                <div className="flex justify-between text-sm">
                  <span className="text-gray-600">Shipping</span>
                  <span className="text-gray-900">${shipping.toFixed(2)}</span>
                </div>
                <div className="flex justify-between text-sm">
                  <span className="text-gray-600">Tax</span>
                  <span className="text-gray-900">${tax.toFixed(2)}</span>
                </div>
                <hr className="border-gray-200" />
                <div className="flex justify-between">
                  <span className="text-lg font-semibold text-gray-900">Total</span>
                  <span className="text-lg font-bold text-indigo-600">${total.toFixed(2)}</span>
                </div>
              </div>

              {/* Place Order Button */}
              <button 
                onClick={handleCheckout}
                disabled={checkoutLoading || isLoading}
                className="w-full py-4 bg-indigo-600 hover:bg-indigo-700 text-white font-semibold rounded-xl transition-colors flex items-center justify-center space-x-2 disabled:opacity-50 disabled:cursor-not-allowed"
              >
                {checkoutLoading ? (
                  <>
                    <svg className="animate-spin h-5 w-5 text-white" fill="none" viewBox="0 0 24 24">
                      <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"></circle>
                      <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                    </svg>
                    <span>Processing...</span>
                  </>
                ) : (
                  <>
                    <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z" />
                    </svg>
                    <span>Pay with Razorpay</span>
                  </>
                )}
              </button>

              {/* Security Note */}
              <p className="mt-4 text-xs text-gray-500 text-center flex items-center justify-center">
                <svg className="w-4 h-4 mr-1" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z" />
                </svg>
                Secure checkout powered by Razorpay
              </p>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default Checkout;
